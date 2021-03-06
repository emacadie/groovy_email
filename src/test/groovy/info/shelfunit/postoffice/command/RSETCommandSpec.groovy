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
import static info.shelfunit.mail.GETestUtils.getTableCount

import groovy.util.logging.Slf4j 

@Slf4j
@Stepwise
class RSETCommandSpec extends Specification {
    
    def crlf = "\r\n"
    static domainList = [ 'shelfunit.info', 'groovy-is-groovy.org' ]
    static sqlObject
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
        sqlObject = ConfigHolder.instance.getSqlObject()
        this.addUsers()
        rsetCommand = new RSETCommand( sqlObject )
    }     // run before the first feature method
    
    def cleanupSpec() {
        sqlObject.execute "DELETE FROM email_user where username in ( ${gwRSET}, ${jaRSET}, ${joRSET} )"
        sqlObject.close()
    }   // run after the last feature method
   
    def addUsers() {
        addUser( sqlObject, 'George', 'Washington', gwRSET, 'somePassword' )
        addUser( sqlObject, 'John', 'Adams', jaRSET, 'somePassword' )
        addUser( sqlObject, 'Jack', "O'Neill", joRSET, 'somePassword' )
        
        addMessage( sqlObject, uuidA, gwRSET, msgA, domainList[ 0 ] )
        addMessage( sqlObject, uuidB, gwRSET, msgB, domainList[ 0 ] )
        addMessage( sqlObject, uuidC, gwRSET, msgC, domainList[ 0 ] )
        theTimestamp = Timestamp.create()
    }
    
    def createDeleteMap() {
        def deleteMap  = [:]
        deleteMap[ 1 ] = uuidA
        deleteMap[ 3 ] = uuidC
        deleteMap[ 2 ] = uuidB
        deleteMap
    }
    
    // @Ignore
    def "test uuid list"() {
        def uuidList = []
        def uuid     = UUID.randomUUID()
        def totalMessageSizeTest = msgA.size() + msgB.size() + msgC.size()
        def bufferInputMap = [:]
        def timestamp
        bufferInputMap.state     = 'TRANSACTION'
        bufferInputMap.timestamp = theTimestamp
        def userInfo      = [:]
	    userInfo.username = gwRSET
        bufferInputMap.userInfo  = userInfo
        bufferInputMap.deleteMap = this.createDeleteMap()
        sleep( 2.seconds() )
        
        when:
            def resultMap = rsetCommand.process( 'RSET', [] as Set, bufferInputMap )
        then:
            resultMap.resultString == "+OK maildrop has 3 messages (66 octets)"
            resultMap.bufferMap.deleteMap.isEmpty()

        def messageStringB = 'aw' * 11
        def toAddress      = "${gwRSET}@${domainList[ 0 ]}".toString()
        when:
            bufferInputMap = resultMap.bufferMap
            bufferInputMap.deleteMap = this.createDeleteMap()
        then:
            !bufferInputMap.deleteMap.isEmpty()
        when:
            def params = [ UUID.randomUUID(), gwRSET, gwRSET.toLowerCase(), 'hello@test.com', toAddress, messageStringB ]
            sqlObject.execute 'insert into mail_store(id, username, username_lc, from_address, to_address, text_body) values (?, ?, ?, ?, ?, ?)', params    
            def messageCount = getTableCount( sqlObject, 'select count(*) from mail_store where username = ?', [ gwRSET ] )
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
        def uuid     = UUID.randomUUID()
        def totalMessageSizeTest = msgA.size() + msgB.size() + msgC.size()
        def bufferInputMap       = [:]
        def timestamp
        bufferInputMap.state     = 'TRANSACTION'
        bufferInputMap.timestamp = theTimestamp
        def userInfo      = [:]
	    userInfo.username = gwRSET
        bufferInputMap.userInfo  = userInfo
        bufferInputMap.deleteMap = this.createDeleteMap()
        sleep( 2.seconds() )
        when:
            def resultMap = rsetCommand.process( 'RSET 1', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "-ERR Command not in proper form"  
            resultMap.bufferMap.deleteMap.containsKey( 1 ) == true
            resultMap.bufferMap.deleteMap.containsKey( 2 ) == true
            resultMap.bufferMap.deleteMap[ 1 ].toString()  == uuidA.toString()
        
        when:
            resultMap = rsetCommand.process( 'RSET 3', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "-ERR Command not in proper form"  
            resultMap.bufferMap.deleteMap.containsKey( 3 ) == true
            resultMap.bufferMap.deleteMap.containsKey( 2 ) == true
            resultMap.bufferMap.deleteMap[ 3 ].toString()  == uuidC.toString()
            
        when:
            resultMap = rsetCommand.process( 'RSET 2', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "-ERR Command not in proper form" 
            resultMap.bufferMap.deleteMap.containsKey( 2 ) == true
            resultMap.bufferMap.deleteMap[ 2 ].toString()  == uuidB.toString()
        
        def messageStringB = 'aw' * 11
        def toAddress = "${gwRSET}@${domainList[ 0 ]}".toString()
        when:
            bufferInputMap = resultMap.bufferMap
            def params = [ UUID.randomUUID(), gwRSET, gwRSET.toLowerCase(), 'hello@test.com', toAddress, messageStringB ]
            sqlObject.execute 'insert into mail_store(id, username, username_lc, from_address, to_address, text_body) values (?, ?, ?, ?, ?, ?)', params    
            def messageCount = getTableCount( sqlObject, 'select count(*) from mail_store where username = ?', [ gwRSET ] )
        then:
            messageCount == 5
        
        when:
            resultMap = rsetCommand.process( 'RSET 4', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "-ERR Command not in proper form" 
            
        // try case-insensitive
        when:
            resultMap = rsetCommand.process( 'RsEt 1', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "-ERR Command not in proper form" 
            log.info "here is resultMap.resultString at end of test: ${resultMap.resultString}"
	}

} // line 193

