package info.shelfunit.postoffice.command

import java.sql.Timestamp

// import spock.lang.Ignore
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
class RETRCommandSpec extends Specification {
    
    def crlf = "\r\n"
    static domainList = [ 'shelfunit.info', 'groovy-is-groovy.org' ]
    static sqlObject
    static retrCommand
    static theTimestamp
    static rString = getRandomString()
    static gwRETR  = 'gw' + rString // @shelfunit.info'
    static jaRETR  = 'ja' + rString // @shelfunit.info'
    static joRETR  = 'jo' + rString // @shelfunit.info'
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
        retrCommand = new RETRCommand( sqlObject )
    }     // run before the first feature method
    
    def cleanupSpec() {
        sqlObject.execute "DELETE FROM email_user where username in ( ${gwRETR}, ${jaRETR}, ${joRETR} )"
        sqlObject.close()
    }   // run after the last feature method
   
    def addUsers() {
        addUser( sqlObject, 'George', 'Washington', gwRETR, 'somePassword' )
        addUser( sqlObject, 'John', 'Adams', jaRETR, 'somePassword' )
        addUser( sqlObject, 'Jack', "O'Neill", joRETR, 'somePassword' )
        
        addMessage( sqlObject, uuidA, gwRETR, msgA, domainList[ 0 ] )
        addMessage( sqlObject, uuidB, gwRETR, msgB, domainList[ 0 ] )
        addMessage( sqlObject, uuidC, gwRETR, msgC, domainList[ 0 ] )
        
        theTimestamp = Timestamp.create()
    }

    def "test uuid list"() {
        def uuidList = []
        def uuid     = UUID.randomUUID()
        def totalMessageSizeTest = msgA.size() + msgB.size() + msgC.size()
        def bufferInputMap = [:]
        def timestamp
        bufferInputMap.state     = 'TRANSACTION'
        bufferInputMap.timestamp = theTimestamp
        def userInfo = [:]
	    userInfo.username        = gwRETR
        bufferInputMap.userInfo = userInfo
        sleep( 2.seconds() )
        when:
            def resultMap = retrCommand.process( 'RETR', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "-ERR Command not in proper form"

        def messageStringB = 'aw' * 11
        def toAddress = "${gwRETR}@${domainList[ 0 ]}".toString()
        when:
            bufferInputMap = resultMap.bufferMap
            def params = [ UUID.randomUUID(), gwRETR, gwRETR.toLowerCase(), 'hello@test.com', toAddress, messageStringB ]
            sqlObject.execute 'insert into mail_store(id, username, username_lc, from_address, to_address, text_body) values (?, ?, ?, ?, ?, ?)', params    
            def messageCount = getTableCount( sqlObject, 'select count(*) from mail_store where username = ?', [ gwRETR ] )
        then:
            messageCount == 4
        when:
            resultMap = retrCommand.process( 'RETR', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "-ERR Command not in proper form"
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
	    userInfo.username = gwRETR
        bufferInputMap.userInfo = userInfo
        sleep( 2.seconds() )
        when:
            def resultMap = retrCommand.process( 'RETR 1', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "+OK ${msgA.size()} octets${crlf}" + "${msgA}${crlf}" + "." 
        
        when:
            resultMap = retrCommand.process( 'RETR 3', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "+OK ${msgC.size()} octets${crlf}" + "${msgC}${crlf}" + "." 
            
        when:
            resultMap = retrCommand.process( 'RETR 2', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "+OK ${msgB.size()} octets${crlf}" + "${msgB}${crlf}" + "." 

        
        def messageStringB = 'aw' * 11
        def toAddress      = "${gwRETR}@${domainList[ 0 ]}".toString()
        when:
            bufferInputMap = resultMap.bufferMap
            def params = [ UUID.randomUUID(), gwRETR, gwRETR.toLowerCase(), 'hello@test.com', toAddress, messageStringB ]
            sqlObject.execute 'insert into mail_store(id, username, username_lc, from_address, to_address, text_body) values (?, ?, ?, ?, ?, ?)', params    
            def messageCount = getTableCount( sqlObject, 'select count(*) from mail_store where username = ?', [ gwRETR ] )
        then:
            messageCount == 5
        
        when:
            resultMap = retrCommand.process( 'RETR 4', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "-ERR no such message, only 3 messages in maildrop"
            
        when:
            resultMap = retrCommand.process( 'RETR 1', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "+OK ${msgA.size()} octets${crlf}" + "${msgA}${crlf}" + "." 
	}

} // line 171

