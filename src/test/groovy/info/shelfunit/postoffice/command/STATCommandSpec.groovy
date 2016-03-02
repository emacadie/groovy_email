package info.shelfunit.postoffice.command

import java.sql.Timestamp

// import spock.lang.Ignore
import spock.lang.Specification
// import spock.lang.Unroll

import org.junit.Rule
import org.junit.rules.TestName

import info.shelfunit.mail.meta.MetaProgrammer
import info.shelfunit.mail.ConfigHolder
import static info.shelfunit.mail.GETestUtils.getBase64Hash

import groovy.sql.Sql
import groovy.util.logging.Slf4j 

import org.apache.shiro.crypto.hash.Sha512Hash

@Slf4j
class STATCommandSpec extends Specification {
    
    def crlf = "\r\n"
    static domainList = [ 'shelfunit.info', 'groovy-is-groovy.org' ]
    static sql
    static iterations = 10000
    static statCommand
    static somePassword = 'somePassword'
    static gwSTAT  = 'gwstat' // @shelfunit.info'
    static jaSTAT = 'jastat' // @shelfunit.info'
    static joSTAT = 'jostat' // @shelfunit.info'

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
        statCommand = new STATCommand( sql )
        
    }     // run before the first feature method
    
    def cleanupSpec() {
        sql.execute "DELETE FROM email_user where username in ( ${gwSTAT}, ${jaSTAT}, ${joSTAT} )"
        sql.close()
    }   // run after the last feature method
   
    def addUsers() {
        def params = [ gwSTAT, ( new Sha512Hash( somePassword, gwSTAT, iterations ).toBase64() ), 'SHA-512', iterations, getBase64Hash( gwSTAT, somePassword ), 'George', 'Washington', 0 ]
        sql.execute 'insert into  email_user( username, password_hash, password_algo, iterations, base_64_hash, first_name, last_name, version ) values ( ?, ?, ?, ?, ?, ?, ?, ? )', params
        
        params = [ jaSTAT, ( new Sha512Hash( somePassword, jaSTAT, iterations ).toBase64() ), 'SHA-512', iterations, getBase64Hash( gwSTAT, somePassword ), 'John', 'Adams', 0 ]
        sql.execute 'insert into  email_user( username, password_hash, password_algo, iterations, base_64_hash, first_name, last_name, version ) values ( ?, ?, ?, ?, ?, ?, ?, ? )', params
        
        params = [ joSTAT, ( new Sha512Hash( somePassword, joSTAT, iterations ).toBase64() ), 'SHA-512', iterations, getBase64Hash( gwSTAT, somePassword ), 'Jack', "O'Neill", 0 ]
        sql.execute 'insert into  email_user( username, password_hash, password_algo, iterations, base_64_hash, first_name, last_name, version ) values ( ?, ?, ?, ?, ?, ?, ?, ? )', params
        // sql.commit()
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
        sleep( 2 * 1000 )
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
            def messageCount
        
            sql.eachRow( 'select count(*) from mail_store where username = ?', [ gwSTAT ] ) { nextRow ->
                messageCount = nextRow.count
            }
        then:
            messageCount == 2
        when:
            resultMap = statCommand.process( 'STAT', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "+OK 1 ${totalMessageSizeTest}"
	}

} // line 126

