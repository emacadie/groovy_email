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
import static info.shelfunit.mail.GETestUtils.addMessage
import static info.shelfunit.mail.GETestUtils.getUserId

import groovy.util.logging.Slf4j 

@Slf4j
@Stepwise
class QUITCommandSpec extends Specification {
    
    def crlf = "\r\n"
    static domainList = [ 'shelfunit.info', 'groovy-is-groovy.org' ]
    static sql
    static quitCommand
    static rString = getRandomString()
    static gwQUIT  = 'gw' + rString // @shelfunit.info'
    static jaQUIT  = 'ja' + rString // @shelfunit.info'
    static joQUIT  = 'jo' + rString // @shelfunit.info'
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
        quitCommand = new QUITCommand( sql, domainList[ 0 ] )
    }     // run before the first feature method
    
    def cleanupSpec() {
        sql.execute "DELETE FROM email_user where username in ( ${gwQUIT}, ${jaQUIT}, ${joQUIT} )"
        sql.close()
    }   // run after the last feature method
   
    def addUsers() {
        addUser( sql, 'George', 'Washington', gwQUIT, 'somePassword', true )
        addUser( sql, 'John', 'Adams', jaQUIT, 'somePassword', true )
        addUser( sql, 'Jack', "O'Neill", joQUIT, 'somePassword', true )
        
        addMessage( sql, uuidA, gwQUIT, msgA, domainList[ 0 ] )
        addMessage( sql, uuidB, gwQUIT, msgB, domainList[ 0 ] )
        addMessage( sql, uuidC, gwQUIT, msgC, domainList[ 0 ] )
        // theTimestamp = Timestamp.create()
    }
    
    // @Ignore
    def "test uuid list"() {
        def uuidList = []
        def uuid = UUID.randomUUID()
        def bufferInputMap = [:]
        def timestamp
        bufferInputMap.state = 'TRANSACTION'
        bufferInputMap.timestamp = Timestamp.create()
        def userInfo = [:]
	    userInfo.username = gwQUIT
	    userInfo.userid = getUserId( sql, gwQUIT )
        bufferInputMap.userInfo = userInfo
        def deleteMap = [ 1: uuidA, 3: uuidC, 2: uuidB ]
        def messageCount = 0
        bufferInputMap.deleteMap = deleteMap
        sleep( 2.seconds() )
        when:
            sql.eachRow( 'select count(*) from mail_store where username = ?', [ gwQUIT ] ) { nextRow ->
                messageCount = nextRow.count
            }
        then:
            messageCount == 3 

        when:
            def resultMap = quitCommand.process( 'QUIT', [] as Set, bufferInputMap )
        then:
            resultMap.resultString == "+OK shelfunit.info POP3 server signing off"
        when:
            sql.eachRow( 'select count(*) from mail_store where username = ?', [ gwQUIT ] ) { nextRow ->
                messageCount = nextRow.count
            }
            def gwLoggedIn = sql.firstRow( 'select logged_in from email_user where username = ?', [ gwQUIT ] )
        then:
            messageCount == 0
            gwLoggedIn.logged_in == false
            
        def messageStringB = 'aw' * 11
        def toAddress = "${gwQUIT}@${domainList[ 0 ]}".toString()
        sql.executeUpdate "UPDATE email_user set logged_in = ? where userid = ?", [ true, userInfo.userid ]
        when:
            bufferInputMap = resultMap.bufferMap
            def newUUID = UUID.randomUUID()  
            addMessage( sql, newUUID, gwQUIT, messageStringB, domainList[ 0 ] )
            sql.eachRow( 'select count(*) from mail_store where username = ?', [ gwQUIT ] ) { nextRow ->
                messageCount = nextRow.count
            }
            gwLoggedIn = sql.firstRow( 'select logged_in from email_user where username = ?', [ gwQUIT ] )
        then:
            messageCount == 1
            gwLoggedIn.logged_in == true
            
         when:
            bufferInputMap.deleteMap = [ 1: newUUID ]
            bufferInputMap.state = 'TRANSACTION'
            userInfo.username = gwQUIT
            userInfo.userid = getUserId( sql, gwQUIT )
            bufferInputMap.userInfo = userInfo
            resultMap = quitCommand.process( 'QUIT', [] as Set, bufferInputMap )
            sql.eachRow( 'select count(*) from mail_store where username = ?', [ gwQUIT ] ) { nextRow ->
                messageCount = nextRow.count
            }
            gwLoggedIn = sql.firstRow( 'select logged_in from email_user where username = ?', [ gwQUIT ] )
         then:
            messageCount == 0
            gwLoggedIn.logged_in == false
	}
	
	def "sending wrong command"() {
	    when:
	        def resultMap = quitCommand.process( 'QUITT', [] as Set, [:] )
	    then:
	        resultMap.resultString == "-ERR Command not in proper form"
	    
	}

    def "In AUTHORIZATION state"() {
        when:
	        def resultMap = quitCommand.process( 'QUIT', [] as Set, [ state: 'AUTHORIZATION' ] )
	    then:
	        resultMap.resultString == "+OK ${domainList[ 0 ]}  POP3 server signing off"
    }

    def "test John Adams"() {
        def uuidList = []
        def uuidJAA = UUID.randomUUID()
        def uuidJAB = UUID.randomUUID()
        addMessage( sql, uuidJAA, jaQUIT, msgA, domainList[ 0 ] )
        addMessage( sql, uuidJAB, jaQUIT, msgB, domainList[ 0 ] )
        def bufferInputMap = [:]
        def timestamp
        bufferInputMap.state = 'TRANSACTION'
        bufferInputMap.timestamp = Timestamp.create()
        def userInfo = [:]
	    userInfo.username = jaQUIT
	    userInfo.userid = getUserId( sql, jaQUIT )
        bufferInputMap.userInfo = userInfo
        def deleteMap = [ 1: uuidJAA, 2: uuidJAB ]
        def messageCount = 0
        bufferInputMap.deleteMap = deleteMap
        def jaLoggedIn
        sleep( 2.seconds() )
        when:
            jaLoggedIn = sql.firstRow( 'select logged_in from email_user where username = ?', [ jaQUIT ] )
        then:
            jaLoggedIn.logged_in == true
        when:
            sql.eachRow( 'select count(*) from mail_store where username = ?', [ jaQUIT ] ) { nextRow ->
                messageCount = nextRow.count
            }
        then:
            messageCount == 2 

        when:
            def resultMap = quitCommand.process( 'QUIT', [] as Set, bufferInputMap )
        then:
            resultMap.resultString == "+OK ${domainList[ 0 ]} POP3 server signing off"
        when:
            sql.eachRow( 'select count(*) from mail_store where username = ?', [ jaQUIT ] ) { nextRow ->
                messageCount = nextRow.count
            }
            jaLoggedIn = sql.firstRow( 'select logged_in from email_user where username = ?', [ jaQUIT ] )
        then:
            messageCount == 0
            jaLoggedIn.logged_in == false
	}
	
	def "test Colonel Jack O'Neill"() {
        def uuidList = []
        def uuidJOA = UUID.randomUUID()
        def uuidJOB = UUID.randomUUID()
        addMessage( sql, uuidJOA, joQUIT, msgA, domainList[ 0 ] )
        addMessage( sql, uuidJOB, joQUIT, msgB, domainList[ 0 ] )
        def bufferInputMap = [:]
        // def timestamp
        bufferInputMap.state = 'TRANSACTION'
        bufferInputMap.timestamp = Timestamp.create()
        def userInfo = [:]
	    userInfo.username = joQUIT
	    userInfo.userid = getUserId( sql, joQUIT )
        bufferInputMap.userInfo = userInfo
        def deleteMap = [ 1: uuidJOA, 2: uuidJOB ]
        def messageCount = 0
        bufferInputMap.deleteMap = deleteMap
        def joLoggedIn
        sleep( 2.seconds() )
        when:
            joLoggedIn = sql.firstRow( 'select logged_in from email_user where username = ?', [ joQUIT ] )
        then:
            joLoggedIn.logged_in == true
        when:
            sql.eachRow( 'select count(*) from mail_store where username = ?', [ joQUIT ] ) { nextRow ->
                messageCount = nextRow.count
            }
        then:
            messageCount == 2 

        when:
            def resultMap = quitCommand.process( 'QUIT', [] as Set, bufferInputMap )
        then:
            resultMap.resultString == "+OK ${domainList[ 0 ]} POP3 server signing off"
        when:
            sql.eachRow( 'select count(*) from mail_store where username = ?', [ joQUIT ] ) { nextRow ->
                messageCount = nextRow.count
            }
            joLoggedIn = sql.firstRow( 'select logged_in from email_user where username = ?', [ joQUIT ] )
        then:
            messageCount == 0
            joLoggedIn.logged_in == false
	}
	
}

