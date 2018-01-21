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
    static sqlObject
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
    static msgD    = 'aw' * 13
    static msgE    = 'kl' * 14
    
    @Rule 
    TestName name = new TestName()
    
    def setup() {
        println "\n--- Starting test ${name.methodName}"
    }          // run before every feature method
    def cleanup() {}        // run after every feature method
    
    def setupSpec() {
        println "rString is ${rString}"
        MetaProgrammer.runMetaProgramming()
        ConfigHolder.instance.setConfObject( "src/test/resources/application.test.conf" )
        def conf  = ConfigHolder.instance.getConfObject()
        sqlObject = ConfigHolder.instance.getSqlObject() 
        this.addUsers()
        listCommand = new LISTCommand( sqlObject )
    }     // run before the first feature method
    
    def cleanupSpec() {
        sqlObject.execute "DELETE FROM email_user where username in ( ${gwLIST}, ${jaLIST}, ${joLIST} )"
        sqlObject.close()
    }   // run after the last feature method

    def addUsers() {
        addUser( sqlObject, 'George', 'Washington', gwLIST, 'somePassword' )
        addUser( sqlObject, 'John',   'Adams',      jaLIST, 'somePassword' )
        addUser( sqlObject, 'Jack',   "O'Neill",    joLIST, 'somePassword' )
        
        addMessage( sqlObject, uuidA, gwLIST, msgA, domainList[ 0 ] )
        addMessage( sqlObject, uuidB, gwLIST, msgB, domainList[ 0 ] )
        addMessage( sqlObject, uuidC, gwLIST, msgC, domainList[ 0 ] )
        theTimestamp = Timestamp.create()
    }

    def "test uuid list"() {
        def uuidList = []
        def uuid     = UUID.randomUUID()
        def totalMessageSizeTest = msgA.size() + msgB.size() + msgC.size()
        def bufferInputMap       = [:]
        def timestamp
        bufferInputMap.state     = 'TRANSACTION'
        bufferInputMap.timestamp = theTimestamp
        def userInfo             = [:]
	    userInfo.username        = gwLIST
        bufferInputMap.userInfo  = userInfo
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
        
        // def messageStringB = 'aw' * 11
        // def toAddress = "${gwLIST}@${domainList[ 0 ]}".toString()
        when:
            bufferInputMap = resultMap.bufferMap
            // this message is added after timestamp, so should not show up in count
            addMessage( sqlObject, UUID.randomUUID(), gwLIST, msgD, domainList[ 0 ] )
            def messageCount = getTableCount( sqlObject, 'select count(*) from mail_store where username = ?', [ gwLIST ] )
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
        def uuid     = UUID.randomUUID()
        def totalMessageSizeTest = msgA.size() + msgB.size() + msgC.size()
        def bufferInputMap       = [:]
        def timestamp
        bufferInputMap.state     = 'TRANSACTION'
        bufferInputMap.timestamp = theTimestamp
        def userInfo             = [:]
	    userInfo.username        = gwLIST
        bufferInputMap.userInfo  = userInfo
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
        
        // def messageStringB = 'kl' * 11
        // def toAddress = "${gwLIST}@${domainList[ 0 ]}".toString()
        when:
            bufferInputMap = resultMap.bufferMap
            // def params = [ UUID.randomUUID(), gwLIST, gwLIST.toLowerCase(), 'hello@test.com', toAddress, messageStringB ]
            // this message is added after timestamp, so should not show up in count
            addMessage( sqlObject, UUID.randomUUID(), gwLIST, msgE, domainList[ 0 ] )
            // sqlObject.execute 'insert into mail_store(id, username, username_lc, from_address, to_address, text_body) values (?, ?, ?, ?, ?, ?)', params    
            def messageCount = getTableCount( sqlObject, 'select count(*) from mail_store where username = ?', [ gwLIST ] )
        then:
            messageCount == 5
        
        when:
            resultMap = listCommand.process( 'LIST 4', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "-ERR no such message, only 3 messages in maildrop"
            
        // go crazy! go case-insensitive!
        when:
            resultMap = listCommand.process( 'LisT', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "+OK ${totalMessageSizeTest}\r\n" +
            "1 ${msgA.size()}\r\n" +
            "2 ${msgB.size()}\r\n" +
            "3 ${msgC.size()}\r\n" +
            "."
	}

    def "test a message with username in mixed case"() {
        def msgF    = 'qw' * 15
        addMessage( sqlObject, UUID.randomUUID(), gwLIST.toUpperCase(), msgF, domainList[ 0 ] )
        sleep( 2.seconds() )
        def bufferInputMap       = [:]
        def timestamp
        bufferInputMap.state     = 'TRANSACTION'
        bufferInputMap.timestamp = Timestamp.create()
        def userInfo             = [:]
	    userInfo.username        = gwLIST
        bufferInputMap.userInfo  = userInfo
        def totalMessageSizeTest = msgA.size() + msgB.size() + msgC.size() + msgD.size() + msgE.size() + msgF.size()
        
        when:
            def resultMap = listCommand.process( 'LisT', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "+OK ${totalMessageSizeTest}\r\n" +
            "1 ${msgA.size()}\r\n" +
            "2 ${msgB.size()}\r\n" +
            "3 ${msgC.size()}\r\n" +
            "4 ${msgD.size()}\r\n" +
            "5 ${msgE.size()}\r\n" +
            "6 ${msgF.size()}\r\n" +
            "."
    }

} // line 182

