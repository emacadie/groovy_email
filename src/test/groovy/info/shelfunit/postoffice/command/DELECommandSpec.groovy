package info.shelfunit.postoffice.command

import java.sql.Timestamp

import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Stepwise
// import spock.lang.Unroll

import org.junit.Rule
import org.junit.rules.TestName

import info.shelfunit.mail.meta.MetaProgrammer
import info.shelfunit.mail.ConfigHolder

import static info.shelfunit.mail.GETestUtils.getRandomString
import static info.shelfunit.mail.GETestUtils.addUser

import groovy.util.logging.Slf4j 

@Slf4j
@Stepwise
class DELECommandSpec extends Specification {
    
    def crlf = "\r\n"
    static domainList = [ 'shelfunit.info', 'groovy-is-groovy.org' ]
    static iterations = 10000
    static deleCommand
    static sql
    static theTimestamp
    static rString = getRandomString()
    static gwDELE  = 'gw' + rString // 'gwdele' // @shelfunit.info'
    static jaDELE  = 'ja' + rString //  'jadele' // @shelfunit.info'
    static joDELE  = 'jo' + rString //'jodele' // @shelfunit.info'
    static uuidA   = UUID.randomUUID()
    static uuidB   = UUID.randomUUID()
    static uuidC   = UUID.randomUUID()
    static msgA    = 'aq' * 10
    static msgB    = 'bq' * 11
    static msgC    = 'cq' * 12
    
    @Rule 
    TestName name = new TestName()
    
    def setup() {
        println "\n--- Starting test ${name.methodName}"
    }          // run before every feature method
    def cleanup() {}        // run after every feature method
    
    def setupSpec() {
        MetaProgrammer.runMetaProgramming()
        ConfigHolder.instance.setConfObject( "src/test/resources/application.test.conf" )
        sql = ConfigHolder.instance.getSqlObject() 
        this.addUsers()
        deleCommand = new DELECommand( sql )
    }     // run before the first feature method
    
    def cleanupSpec() {
        sql.execute "DELETE FROM email_user where username in ( ${gwDELE}, ${jaDELE}, ${joDELE} )"
        sql.close()
    }   // run after the last feature method
    
    def addUsers() {
        addUser( sql, 'George', 'Washington', gwDELE, 'somePassword' )
        addUser( sql, 'John', 'Adams', jaDELE, 'somePassword' )
        addUser( sql, 'Jack', "O'Neill", joDELE, 'somePassword' )
        
        this.addMessage( uuidA, gwDELE, msgA )
        this.addMessage( uuidB, gwDELE, msgB )
        this.addMessage( uuidC, gwDELE, msgC )
        theTimestamp = Timestamp.create()
    }
    
    def addMessage( uuid, userName, messageString ) {
        def toAddress = "${userName}@${domainList[ 0 ]}".toString()
        def params = [ uuid, userName, 'hello@test.com', toAddress, messageString ]
        sql.execute 'insert into mail_store(id, username, from_address, to_address, text_body) values (?, ?, ?, ?, ?)', params
    }
    
    // @Ignore
    def "test uuid list"() {
        def uuidList = []
        def uuid = UUID.randomUUID()
        def totalMessageSizeTest = msgA.size() + msgB.size() + msgC.size()
        def bufferInputMap = [:]
        def timestamp
        bufferInputMap.state = 'TRANSACTION'
        bufferInputMap.timestamp = theTimestamp
        def userInfo = [:]
	    userInfo.username = gwDELE
        bufferInputMap.userInfo = userInfo
        sleep( 2.seconds() )
        when:
            def resultMap = deleCommand.process( 'DELE', [] as Set, bufferInputMap )
        then:
            resultMap.resultString == "-ERR Command not in proper form"

        def messageStringB = 'aw' * 11
        def toAddress = "${gwDELE}@${domainList[ 0 ]}".toString()
        when:
            bufferInputMap = resultMap.bufferMap
            def params = [ UUID.randomUUID(), gwDELE, 'hello@test.com', toAddress, messageStringB ]
            sql.execute 'insert into mail_store(id, username, from_address, to_address, text_body) values (?, ?, ?, ?, ?)', params    
            def messageCount
        
            sql.eachRow( 'select count(*) from mail_store where username = ?', [ gwDELE ] ) { nextRow ->
                messageCount = nextRow.count
            }
        then:
            messageCount == 4
        when:
            resultMap = deleCommand.process( 'DELE', [] as Set, bufferInputMap )
        then:
            resultMap.resultString == "-ERR Command not in proper form"
	}

	def "test individual messages"() {
        def uuidList = []
        def uuid = UUID.randomUUID()
        def totalMessageSizeTest = msgA.size() + msgB.size() + msgC.size()
        def bufferInputMap = [:]
        def timestamp
        bufferInputMap.state = 'TRANSACTION'
        bufferInputMap.timestamp = theTimestamp
        def userInfo = [:]
	    userInfo.username = gwDELE
        bufferInputMap.userInfo = userInfo
        sleep( 2.seconds() )
        when:
            def resultMap = deleCommand.process( 'DELE 1', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "+OK message 1 deleted"  
            resultMap.bufferMap.deleteMap.containsKey( 1 ) == true
            resultMap.bufferMap.deleteMap.containsKey( 2 ) == false
            resultMap.bufferMap.deleteMap[ 1 ].toString() == uuidA.toString()
        
        when:
            resultMap = deleCommand.process( 'DELE 3', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "+OK message 3 deleted"  
            resultMap.bufferMap.deleteMap.containsKey( 3 ) == true
            resultMap.bufferMap.deleteMap.containsKey( 2 ) == false
            resultMap.bufferMap.deleteMap[ 3 ].toString() == uuidC.toString()
            
        when:
            resultMap = deleCommand.process( 'DELE 2', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "+OK message 2 deleted" 
            resultMap.bufferMap.deleteMap.containsKey( 2 ) == true
            resultMap.bufferMap.deleteMap[ 2 ].toString() == uuidB.toString()
        
        def messageStringB = 'aw' * 11
        def toAddress = "${gwDELE}@${domainList[ 0 ]}".toString()
        when:
            bufferInputMap = resultMap.bufferMap
            def params = [ UUID.randomUUID(), gwDELE, 'hello@test.com', toAddress, messageStringB ]
            sql.execute 'insert into mail_store(id, username, from_address, to_address, text_body) values (?, ?, ?, ?, ?)', params    
            def messageCount
        
            sql.eachRow( 'select count(*) from mail_store where username = ?', [ gwDELE ] ) { nextRow ->
                messageCount = nextRow.count
            }
        then:
            messageCount == 5
        
        when:
            resultMap = deleCommand.process( 'DELE 4', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "-ERR no such message, only 3 messages in maildrop"
            
        when:
            resultMap = deleCommand.process( 'DELE 1', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString != "+OK ${msgA.size()} octets${crlf}" + "${msgA}${crlf}" + "."
            log.info "here is resultMap.resultString at end of test: ${resultMap.resultString}"
	}

} // line 199


