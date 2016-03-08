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
import static info.shelfunit.mail.GETestUtils.addUser
import static info.shelfunit.mail.GETestUtils.addMessage
import static info.shelfunit.mail.GETestUtils.getRandomString

import groovy.util.logging.Slf4j 

@Slf4j
@Stepwise
class RSETCommandSpec extends Specification {
    
    def crlf = "\r\n"
    static domainList = [ 'shelfunit.info', 'groovy-is-groovy.org' ]
    static sql
    static rsetCommand
    static theTimestamp
    static rString = getRandomString()
    static gwRSET  = 'gw' + rString // @shelfunit.info'
    static jaRSET  = 'ja' + rString // @shelfunit.info'
    static joRSET  = 'jo' + rString // @shelfunit.info'
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
        rsetCommand = new RSETCommand( sql )
    }     // run before the first feature method
    
    def cleanupSpec() {
        sql.execute "DELETE FROM email_user where username in ( ${gwRSET}, ${jaRSET}, ${joRSET} )"
        sql.close()
    }   // run after the last feature method
   
    def addUsers() {
        addUser( sql, 'George', 'Washington', gwRSET, 'somePassword' )
        addUser( sql, 'John', 'Adams', jaRSET, 'somePassword' )
        addUser( sql, 'Jack', "O'Neill", joRSET, 'somePassword' )
        
        addMessage( sql, uuidA, gwRSET, msgA, domainList[ 0 ] )
        addMessage( sql, uuidB, gwRSET, msgB, domainList[ 0 ] )
        addMessage( sql, uuidC, gwRSET, msgC, domainList[ 0 ] )
        theTimestamp = Timestamp.create()
    }
    
    def createDeleteMap() {
        def deleteMap = [:]
        deleteMap[ 1 ] = uuidA
        deleteMap[ 3 ] = uuidC
        deleteMap[ 2 ] = uuidB
        deleteMap
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
	    userInfo.username = gwRSET
        bufferInputMap.userInfo = userInfo
        bufferInputMap.deleteMap = this.createDeleteMap()
        sleep( 2.seconds() )
        
        when:
            def resultMap = rsetCommand.process( 'RSET', [] as Set, bufferInputMap )
        then:
            resultMap.resultString == "+OK maildrop has 3 messages (66 octets)"
            resultMap.bufferMap.deleteMap.isEmpty()

        def messageStringB = 'aw' * 11
        def toAddress = "${gwRSET}@${domainList[ 0 ]}".toString()
        when:
            bufferInputMap = resultMap.bufferMap
            bufferInputMap.deleteMap = this.createDeleteMap()
        then:
            !bufferInputMap.deleteMap.isEmpty()
        when:
            def params = [ UUID.randomUUID(), gwRSET, 'hello@test.com', toAddress, messageStringB ]
            sql.execute 'insert into mail_store(id, username, from_address, to_address, text_body) values (?, ?, ?, ?, ?)', params    
            def messageCount
        
            sql.eachRow( 'select count(*) from mail_store where username = ?', [ gwRSET ] ) { nextRow ->
                messageCount = nextRow.count
            }
        then:
            messageCount == 4
        when:
            resultMap = rsetCommand.process( 'RSET', [] as Set, bufferInputMap )
        then:
            resultMap.resultString == "+OK maildrop has 3 messages (66 octets)"
            resultMap.bufferMap.deleteMap.isEmpty()
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
	    userInfo.username = gwRSET
        bufferInputMap.userInfo = userInfo
        bufferInputMap.deleteMap = this.createDeleteMap()
        sleep( 2.seconds() )
        when:
            def resultMap = rsetCommand.process( 'RSET 1', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "-ERR Command not in proper form"  
            resultMap.bufferMap.deleteMap.containsKey( 1 ) == true
            resultMap.bufferMap.deleteMap.containsKey( 2 ) == true
            resultMap.bufferMap.deleteMap[ 1 ].toString() == uuidA.toString()
        
        when:
            resultMap = rsetCommand.process( 'RSET 3', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "-ERR Command not in proper form"  
            resultMap.bufferMap.deleteMap.containsKey( 3 ) == true
            resultMap.bufferMap.deleteMap.containsKey( 2 ) == true
            resultMap.bufferMap.deleteMap[ 3 ].toString() == uuidC.toString()
            
        when:
            resultMap = rsetCommand.process( 'RSET 2', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "-ERR Command not in proper form" 
            resultMap.bufferMap.deleteMap.containsKey( 2 ) == true
            resultMap.bufferMap.deleteMap[ 2 ].toString() == uuidB.toString()
        
        def messageStringB = 'aw' * 11
        def toAddress = "${gwRSET}@${domainList[ 0 ]}".toString()
        when:
            bufferInputMap = resultMap.bufferMap
            def params = [ UUID.randomUUID(), gwRSET, 'hello@test.com', toAddress, messageStringB ]
            sql.execute 'insert into mail_store(id, username, from_address, to_address, text_body) values (?, ?, ?, ?, ?)', params    
            def messageCount
        
            sql.eachRow( 'select count(*) from mail_store where username = ?', [ gwRSET ] ) { nextRow ->
                messageCount = nextRow.count
            }
        then:
            messageCount == 5
        
        when:
            resultMap = rsetCommand.process( 'RSET 4', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "-ERR Command not in proper form" 
            
        when:
            resultMap = rsetCommand.process( 'RSET 1', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "-ERR Command not in proper form" 
            log.info "here is resultMap.resultString at end of test: ${resultMap.resultString}"
	}

}

