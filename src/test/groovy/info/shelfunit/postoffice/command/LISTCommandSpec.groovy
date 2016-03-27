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
class LISTCommandSpec extends Specification {
    
    def crlf = "\r\n"
    static domainList = [ 'shelfunit.info', 'groovy-is-groovy.org' ]
    static listCommand
    static sql
    static theTimestamp
    static rString = getRandomString()
    static gwLIST  = 'gw' + rString // @shelfunit.info'
    static jaLIST  = 'ja' + rString // @shelfunit.info'
    static joLIST  = 'jo' + rString // @shelfunit.info'
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
        def conf = ConfigHolder.instance.getConfObject()
        sql = ConfigHolder.instance.getSqlObject() 
        this.addUsers()
        listCommand = new LISTCommand( sql )
    }     // run before the first feature method
    
    def cleanupSpec() {
        sql.execute "DELETE FROM email_user where username in ( ${gwLIST}, ${jaLIST}, ${joLIST} )"
        sql.close()
    }   // run after the last feature method

    def addUsers() {
        addUser( sql, 'George', 'Washington', gwLIST, 'somePassword' )
        addUser( sql, 'John', 'Adams', jaLIST, 'somePassword' )
        addUser( sql, 'Jack', "O'Neill", joLIST, 'somePassword' )
        
        addMessage( sql, uuidA, gwLIST, msgA, domainList[ 0 ] )
        addMessage( sql, uuidB, gwLIST, msgB, domainList[ 0 ] )
        addMessage( sql, uuidC, gwLIST, msgC, domainList[ 0 ] )
        theTimestamp = Timestamp.create()
    }

    def "test uuid list"() {
        def uuidList = []
        def uuid = UUID.randomUUID()
        def totalMessageSizeTest = msgA.size() + msgB.size() + msgC.size()
        def bufferInputMap = [:]
        def timestamp
        bufferInputMap.state = 'TRANSACTION'
        bufferInputMap.timestamp = theTimestamp
        def userInfo = [:]
	    userInfo.username = gwLIST
        bufferInputMap.userInfo = userInfo
        sleep( 2.seconds() )
        when:
            def resultMap = listCommand.process( 'LIST', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "+OK ${totalMessageSizeTest}\r\n" +
            "1 ${msgA.size()}\r\n" +
            "2 ${msgB.size()}\r\n" +
            "3 ${msgC.size()}\r\n" +
            "."
        
        def messageStringB = 'aw' * 11
        def toAddress = "${gwLIST}@${domainList[ 0 ]}".toString()
        when:
            bufferInputMap = resultMap.bufferMap
            def params = [ UUID.randomUUID(), gwLIST, 'hello@test.com', toAddress, messageStringB ]
            sql.execute 'insert into mail_store(id, username, from_address, to_address, text_body) values (?, ?, ?, ?, ?)', params    
            def messageCount = getTableCount( sql, 'select count(*) from mail_store where username = ?', [ gwLIST ] )
        then:
            messageCount == 4
        when:
            resultMap = listCommand.process( 'LIST', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "+OK ${totalMessageSizeTest}\r\n" +
            "1 ${msgA.size()}\r\n" +
            "2 ${msgB.size()}\r\n" +
            "3 ${msgC.size()}\r\n" +
            "."
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
	    userInfo.username = gwLIST
        bufferInputMap.userInfo = userInfo
        sleep( 2.seconds() )
        when:
            def resultMap = listCommand.process( 'LIST 1', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "+OK 1 ${msgA.size()}"
        
        when:
            resultMap = listCommand.process( 'LIST 3', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "+OK 3 ${msgC.size()}"
            
        when:
            resultMap = listCommand.process( 'LIST 2', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "+OK 2 ${msgB.size()}"
        
        def messageStringB = 'aw' * 11
        def toAddress = "${gwLIST}@${domainList[ 0 ]}".toString()
        when:
            bufferInputMap = resultMap.bufferMap
            def params = [ UUID.randomUUID(), gwLIST, 'hello@test.com', toAddress, messageStringB ]
            sql.execute 'insert into mail_store(id, username, from_address, to_address, text_body) values (?, ?, ?, ?, ?)', params    
            def messageCount = getTableCount( sql, 'select count(*) from mail_store where username = ?', [ gwLIST ] )
        then:
            messageCount == 5
        
        when:
            resultMap = listCommand.process( 'LIST 4', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "-ERR no such message, only 3 messages in maildrop"
            
        when:
            resultMap = listCommand.process( 'LIST', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "+OK ${totalMessageSizeTest}\r\n" +
            "1 ${msgA.size()}\r\n" +
            "2 ${msgB.size()}\r\n" +
            "3 ${msgC.size()}\r\n" +
            "."
	}

} // line 182

