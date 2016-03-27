package info.shelfunit.postoffice.command

import java.sql.Timestamp

// import spock.lang.Ignore
import spock.lang.Specification
// import spock.lang.Unroll

import org.junit.Rule
import org.junit.rules.TestName

import info.shelfunit.mail.meta.MetaProgrammer
import info.shelfunit.mail.ConfigHolder
import static info.shelfunit.mail.GETestUtils.addUser
import static info.shelfunit.mail.GETestUtils.getRandomString
import static info.shelfunit.mail.GETestUtils.getTableCount

import groovy.util.logging.Slf4j 

@Slf4j
class STATCommandSpec extends Specification {
    
    def crlf = "\r\n"
    static domainList = [ 'shelfunit.info', 'groovy-is-groovy.org' ]
    static sql
    static statCommand
    static rString = getRandomString()
    static gwSTAT  = 'gwstat' + rString // @shelfunit.info'
    static jaSTAT  = 'jastat' + rString // @shelfunit.info'
    static joSTAT  = 'jostat' + rString // @shelfunit.info'

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
        statCommand = new STATCommand( sql )
    }     // run before the first feature method
    
    def cleanupSpec() {
        sql.execute "DELETE FROM email_user where username in ( ${gwSTAT}, ${jaSTAT}, ${joSTAT} )"
        sql.close()
    }   // run after the last feature method
   
    def addUsers() {
        addUser( sql, 'George', 'Washington', gwSTAT, 'somePassword' )
        addUser( sql, 'John', 'Adams', jaSTAT, 'somePassword' )
        addUser( sql, 'Jack', "O'Neill", joSTAT, 'somePassword' )
    }

    def "test uuid list"() {
        def uuidList = []
        def uuid = UUID.randomUUID()
        def totalMessageSizeTest = 0
        def messageString = 'aq' * 10
        totalMessageSizeTest = messageString.size()
        def toAddress = "${gwSTAT}@${domainList[ 0 ]}".toString()
        def params = [ uuid, gwSTAT, 'hello@test.com', toAddress, messageString ]
        sql.execute 'insert into mail_store(id, username, from_address, to_address, text_body) values (?, ?, ?, ?, ?)', params
        def bufferInputMap = [:]
        def timestamp
        bufferInputMap.state = 'TRANSACTION'
        bufferInputMap.timestamp = Timestamp.create()
        def userInfo = [:]
	    userInfo.username = gwSTAT
        bufferInputMap.userInfo = userInfo
        sleep( 2.seconds() )
        when:
            def resultMap = statCommand.process( 'STAT', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "+OK 1 ${totalMessageSizeTest}"
        
        def messageStringB = 'aw' * 11

        when:
            bufferInputMap = resultMap.bufferMap
            params = [ UUID.randomUUID(), gwSTAT, 'hello@test.com', toAddress, messageStringB ]
            sql.execute 'insert into mail_store(id, username, from_address, to_address, text_body) values (?, ?, ?, ?, ?)', params    
            def messageCount = getTableCount( sql, 'select count(*) from mail_store where username = ?', [ gwSTAT ] )
        then:
            messageCount == 2
        when:
            resultMap = statCommand.process( 'STAT', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "+OK 1 ${totalMessageSizeTest}"
	}

} // line 101

