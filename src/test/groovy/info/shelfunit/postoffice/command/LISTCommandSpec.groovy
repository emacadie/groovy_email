package info.shelfunit.postoffice.command

import java.sql.Timestamp

import spock.lang.Ignore
import spock.lang.Specification
// import spock.lang.Unroll

import org.junit.Rule
import org.junit.rules.TestName

import info.shelfunit.mail.MetaProgrammer
import info.shelfunit.mail.ConfigHolder

import groovy.sql.Sql
import groovy.util.logging.Slf4j 

import org.apache.shiro.crypto.hash.Sha512Hash

@Slf4j
class LISTCommandSpec extends Specification {
    
    def crlf = "\r\n"
    static domainList = [ 'shelfunit.info', 'groovy-is-groovy.org' ]
    static sql
    static iterations = 10000
    static listCommand
    static somePassword = 'somePassword'
    static hamilton = 'alexander' // @shelfunit.info'
    static gwLIST  = 'gwlist' // @shelfunit.info'
    static jaLIST = 'jalist' // @shelfunit.info'
    static joLIST = 'jolist' // @shelfunit.info'
    static gwGroovy  = 'gwlist' // @groovy-is-groovy.org'
    static jaGroovy  = 'jalist' // @groovy-is-groovy.org'
    static jackGroovy = 'jolist' // @groovy-is-groovy.org'
    static resultSetEMR = [ 'EHLO', 'MAIL', 'RCPT' ] as Set
    static resultSetEM  = [ 'EHLO', 'MAIL' ] as Set 

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
        listCommand = new LISTCommand( sql )
        
    }     // run before the first feature method
    
    def cleanupSpec() {
        sql.execute "DELETE FROM email_user where username in ( ${gwLIST}, ${jaLIST}, ${joLIST} )"
        sql.close()
    }   // run after the last feature method
   
    def addUsers() {
        def params = [ gwLIST, ( new Sha512Hash( somePassword, gwLIST, iterations ).toBase64() ), 'SHA-512', iterations, 'George', 'Washington', 0 ]
        sql.execute 'insert into  email_user( username, password_hash, password_algo, iterations, first_name, last_name, version ) values ( ?, ?, ?, ?, ?, ?, ? )', params
        
        params = [ jaLIST, ( new Sha512Hash( somePassword, jaLIST, iterations ).toBase64() ), 'SHA-512', iterations, 'John', 'Adams', 0 ]
        sql.execute 'insert into  email_user( username, password_hash, password_algo, iterations, first_name, last_name, version ) values ( ?, ?, ?, ?, ?, ?, ? )', params
        
        params = [ joLIST, ( new Sha512Hash( somePassword, joLIST, iterations ).toBase64() ), 'SHA-512', iterations, 'Jack', "O'Neill", 0 ]
        sql.execute 'insert into  email_user( username, password_hash, password_algo, iterations, first_name, last_name, version ) values ( ?, ?, ?, ?, ?, ?, ? )', params
        // sql.commit()
    }

    def "test uuid list"() {
        def uuidList = []
        def uuid = UUID.randomUUID()
        def totalMessageSizeTest = 0
        def messageString = 'aq' * 10
        totalMessageSizeTest = messageString.size()
        def toAddress = "${gwLIST}@${domainList[ 0 ]}".toString()
        def params = [ uuid, gwLIST, 'hello@test.com', toAddress, messageString ]
        sql.execute 'insert into mail_store(id, username, from_address, to_address, text_body) values (?, ?, ?, ?, ?)', params
        def bufferInputMap = [:]
        def timestamp
        bufferInputMap.state = 'TRANSACTION'
        bufferInputMap.timestamp = Timestamp.create()
        def userInfo = [:]
	    userInfo.username = gwLIST
        bufferInputMap.userInfo = userInfo
        sleep( 2 * 1000 )
        when:
            def resultMap = listCommand.process( 'LIST', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "+OK 1 ${totalMessageSizeTest}"
        
            
        def messageStringB = 'aw' * 11
        // totalMessageSizeTest = messageString.size()
        // def toAddress = "${gwLIST}@${domainList[ 0 ]}".toString()
        when:
            bufferInputMap = resultMap.bufferMap
            params = [ UUID.randomUUID(), gwLIST, 'hello@test.com', toAddress, messageStringB ]
            sql.execute 'insert into mail_store(id, username, from_address, to_address, text_body) values (?, ?, ?, ?, ?)', params    
            def messageCount
        
            sql.eachRow( 'select count(*) from mail_store where username = ?', [ gwLIST ] ) { nextRow ->
                messageCount = nextRow.count
            }
        then:
            messageCount == 2
        when:
            resultMap = listCommand.process( 'LIST', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "+OK 1 ${totalMessageSizeTest}"
        // timestamp = resultMap.bufferMap.timestamp
        // insertCounts = sql.withBatch(  )
	}

	@Ignore
	def "the first password attempt"() {
	    def userInfo = [:]
	    userInfo.username = "some.user"
	    def password = "this.is.a.password"
	    userInfo.password_algo = 'SHA-512'
	    userInfo.iterations = iterations
	    def rawHash = new Sha512Hash( password, userInfo.username, userInfo.iterations ) 
	    userInfo.password_hash = rawHash.toBase64()
	    userInfo.first_name = 'some'
	    userInfo.last_name  = 'user'
	    userInfo.userid = 10
	    
	    def bufferMap = [:]
	    bufferMap.state = 'AUTHORIZATION'
	    bufferMap.userInfo = userInfo
	    def prevCommandSet = [] as Set
        def resultMap
	    def resultString
	    when:
	        resultMap = listCommand.process( "PASS ${password}", resultSetEM,  bufferMap )
	    then:
	        resultMap.resultString == "+OK ${userInfo.username} authenticated"
	        resultMap.bufferMap.state == 'TRANSACTION'
	        
	    when:
	        bufferMap.state = 'AUTHORIZATION'
	        resultMap = listCommand.process( "PASS tdsgghrd", resultSetEM,  bufferMap )
	    then:
	        resultMap.resultString == "-ERR ${userInfo.username} not authenticated"
	        resultMap.bufferMap.state == 'AUTHORIZATION'
	}

}

