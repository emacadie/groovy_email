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

import groovy.sql.Sql
import groovy.util.logging.Slf4j 

import org.apache.shiro.crypto.hash.Sha512Hash

@Slf4j
@Stepwise
class QUITCommandSpec extends Specification {
    
    def crlf = "\r\n"
    static domainList = [ 'shelfunit.info', 'groovy-is-groovy.org' ]
    static sql
    static iterations = 10000
    static quitCommand
    static somePassword = 'somePassword'
    static theTimestamp
    static gwQUIT  = 'gwquit' // @shelfunit.info'
    static jaQUIT = 'jaquit' // @shelfunit.info'
    static joQUIT = 'joquit' // @shelfunit.info'
    static uuidA = UUID.randomUUID()
    static uuidB = UUID.randomUUID()
    static uuidC = UUID.randomUUID()
    static msgA = 'aq' * 10
    static msgB = 'bq' * 11
    static msgC = 'cq' * 12
    
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
        log.info "conf is a ${conf.class.name}"
        def db = [ url: "jdbc:postgresql://${conf.database.host_and_port}/${conf.database.dbname}",
        user: conf.database.dbuser, password: conf.database.dbpassword, driver: conf.database.driver ]
        log.info "db is a ${db.getClass().name}"
        sql = Sql.newInstance( db.url, db.user, db.password, db.driver )
        this.addUsers()
        quitCommand = new QUITCommand( sql, domainList[ 0 ] )
        
    }     // run before the first feature method
    
    def cleanupSpec() {
        sql.execute "DELETE FROM email_user where username in ( ${gwQUIT}, ${jaQUIT}, ${joQUIT} )"
        sql.close()
    }   // run after the last feature method
   
    def addUsers() {
        def params = [ gwQUIT, ( new Sha512Hash( somePassword, gwQUIT, iterations ).toBase64() ), 'SHA-512', iterations, 'George', 'Washington', 0 ]
        sql.execute 'insert into  email_user( username, password_hash, password_algo, iterations, first_name, last_name, version ) values ( ?, ?, ?, ?, ?, ?, ? )', params
        
        params = [ jaQUIT, ( new Sha512Hash( somePassword, jaQUIT, iterations ).toBase64() ), 'SHA-512', iterations, 'John', 'Adams', 0 ]
        sql.execute 'insert into  email_user( username, password_hash, password_algo, iterations, first_name, last_name, version ) values ( ?, ?, ?, ?, ?, ?, ? )', params
        
        params = [ joQUIT, ( new Sha512Hash( somePassword, joQUIT, iterations ).toBase64() ), 'SHA-512', iterations, 'Jack', "O'Neill", 0 ]
        sql.execute 'insert into  email_user( username, password_hash, password_algo, iterations, first_name, last_name, version ) values ( ?, ?, ?, ?, ?, ?, ? )', params
        // sql.commit()
        this.addMessage( uuidA, gwQUIT, msgA )
        this.addMessage( uuidB, gwQUIT, msgB )
        this.addMessage( uuidC, gwQUIT, msgC )
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
	    userInfo.username = gwQUIT
        bufferInputMap.userInfo = userInfo
        def deleteMap = [ 1: uuidA, 3: uuidC, 2: uuidB ]
        bufferInputMap.deleteMap = deleteMap
        sleep( 2 * 1000 )
        when:
            def resultMap = quitCommand.process( 'QUIT', [] as Set, bufferInputMap )
        then:
            resultMap.resultString == "+OK shelfunit.info POP3 server signing off"

        def messageStringB = 'aw' * 11
        def toAddress = "${gwQUIT}@${domainList[ 0 ]}".toString()
        when:
            bufferInputMap = resultMap.bufferMap
            def params = [ UUID.randomUUID(), gwQUIT, 'hello@test.com', toAddress, messageStringB ]
            sql.execute 'insert into mail_store(id, username, from_address, to_address, text_body) values (?, ?, ?, ?, ?)', params    
            def messageCount
        
            sql.eachRow( 'select count(*) from mail_store where username = ?', [ gwQUIT ] ) { nextRow ->
                messageCount = nextRow.count
            }
        then:
            messageCount == 1
	}

}

