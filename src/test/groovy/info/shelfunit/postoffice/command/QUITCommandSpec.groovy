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
class QUITCommandSpec extends Specification {
    
    def crlf = "\r\n"
    static domainList = [ 'shelfunit.info', 'groovy-is-groovy.org' ]
    static sql
    static quitCommand
    // static theTimestamp
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
        addUser( sql, 'George', 'Washington', gwQUIT, 'somePassword' )
        addUser( sql, 'John', 'Adams', jaQUIT, 'somePassword' )
        addUser( sql, 'Jack', "O'Neill", joQUIT, 'somePassword' )
        
        this.addMessage( uuidA, gwQUIT, msgA )
        this.addMessage( uuidB, gwQUIT, msgB )
        this.addMessage( uuidC, gwQUIT, msgC )
        // theTimestamp = Timestamp.create()
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
        bufferInputMap.timestamp = Timestamp.create()
        def userInfo = [:]
	    userInfo.username = gwQUIT
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
        then:
            messageCount == 0
            
        def messageStringB = 'aw' * 11
        def toAddress = "${gwQUIT}@${domainList[ 0 ]}".toString()
        when:
            bufferInputMap = resultMap.bufferMap
            def params = [ UUID.randomUUID(), gwQUIT, 'hello@test.com', toAddress, messageStringB ]
            sql.execute 'insert into mail_store(id, username, from_address, to_address, text_body) values (?, ?, ?, ?, ?)', params    
            
        
            sql.eachRow( 'select count(*) from mail_store where username = ?', [ gwQUIT ] ) { nextRow ->
                messageCount = nextRow.count
            }
        then:
            messageCount == 1
	}

}

