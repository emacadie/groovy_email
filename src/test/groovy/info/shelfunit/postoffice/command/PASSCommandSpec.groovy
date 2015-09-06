package info.shelfunit.postoffice.command

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
class PASSCommandSpec extends Specification {
    
    def crlf = "\r\n"
    static domainList = [ 'shelfunit.info', 'groovy-is-groovy.org' ]
    static sql
    static iterations = 10000
    static passCommand
    static hamilton = 'alexander' // @shelfunit.info'
    static gwShelf  = 'george.washingtonp' // @shelfunit.info'
    static jAdamsShelf = 'john.adamsp' // @shelfunit.info'
    static jackShelf = 'oneillp' // @shelfunit.info'
    static gwGroovy  = 'george.washingtonp' // @groovy-is-groovy.org'
    static jaGroovy  = 'john.adamsp' // @groovy-is-groovy.org'
    static jackGroovy = 'oneillp' // @groovy-is-groovy.org'
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
        // this.addUsers()
        passCommand = new PASSCommand( sql )
        
    }     // run before the first feature method
    
    def cleanupSpec() {
        // sql.execute "DELETE FROM email_user where username in ('george.washingtonp', 'john.adamsp', 'oneillp')"
        // sql.close()
    }   // run after the last feature method
   
    def addUsers() {
        def numIterations = 10000
        def salt = 'you say your password tastes like chicken? Add salt!'
        def atx512 = new Sha512Hash( 'somePassword', salt, 1000000 )
        def params = [ 'george.washingtonp', atx512.toBase64(), 'SHA-512', numIterations, 'George', 'Washington', 0 ]
        sql.execute 'insert into  email_user( username, password_hash, password_algo, iterations, first_name, last_name, version ) values ( ?, ?, ?, ?, ?, ?, ? )', params
        
        params = [ 'john.adamsp', atx512.toBase64(), 'SHA-512', numIterations, 'John', 'Adams', 0 ]
        sql.execute 'insert into  email_user( username, password_hash, password_algo, iterations, first_name, last_name, version ) values ( ?, ?, ?, ?, ?, ?, ? )', params
        
        params = [ 'oneillp', atx512.toBase64(), 'SHA-512', numIterations, 'Jack', "O'Neill", 0 ]
        sql.execute 'insert into  email_user( username, password_hash, password_algo, iterations, first_name, last_name, version ) values ( ?, ?, ?, ?, ?, ?, ? )', params
        // sql.commit()
    }
    
    def "test wrong buffer state"() {
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
	    bufferMap.state = 'TRANSACTION'
	    bufferMap.userInfo = userInfo
	    def prevCommandSet = [] as Set
        def resultMap
	    def resultString
	    when:
	        resultMap = passCommand.process( "PASS ${password}", resultSetEM,  bufferMap )
	    then:
	        resultMap.resultString == "-ERR Not in AUTHORIZATION state"
	        resultMap.bufferMap.state == 'TRANSACTION'
	        
	    when:
	        bufferMap.state = 'AUTHORIZATION'
	        resultMap = passCommand.process( "PASS tdsgghrd", resultSetEM,  bufferMap )
	    then:
	        resultMap.resultString == "-ERR ${userInfo.username} not authenticated"
	        resultMap.bufferMap.state == 'AUTHORIZATION'
	}

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
	        resultMap = passCommand.process( "PASS ${password}", resultSetEM,  bufferMap )
	    then:
	        resultMap.resultString == "+OK ${userInfo.username} authenticated"
	        resultMap.bufferMap.state == 'TRANSACTION'
	        resultMap.bufferMap.timestamp.class.name == 'java.sql.Timestamp'
	        
	    when:
	        bufferMap.state = 'AUTHORIZATION'
	        resultMap = passCommand.process( "PASS tdsgghrd", resultSetEM,  bufferMap )
	    then:
	        resultMap.resultString == "-ERR ${userInfo.username} not authenticated"
	        resultMap.bufferMap.state == 'AUTHORIZATION'
	        resultMap.bufferMap.timestamp == null
	}
	
	def "bad password does not change state or set timestamp"() {
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
	        bufferMap.state = 'AUTHORIZATION'
	        resultMap = passCommand.process( "PASS tdsgghrd", resultSetEM,  bufferMap )
	    then:
	        resultMap.resultString == "-ERR ${userInfo.username} not authenticated"
	        resultMap.bufferMap.state == 'AUTHORIZATION'
	        resultMap.bufferMap.timestamp == null
	}

}

