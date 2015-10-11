package info.shelfunit.postoffice.command

import spock.lang.Specification
import spock.lang.Unroll

import org.junit.Rule
import org.junit.rules.TestName

import info.shelfunit.mail.meta.MetaProgrammer
import info.shelfunit.mail.ConfigHolder

import groovy.sql.Sql
import groovy.util.logging.Slf4j 

import org.apache.shiro.crypto.hash.Sha512Hash

@Slf4j
class USERCommandSpec extends Specification {
    
    def crlf = "\r\n"
    
    static domainList = [ 'shelfunit.info', 'groovy-is-groovy.org' ]
    static sql
    static userCommand
    static gwShelf  = 'george.washingtonu' // @shelfunit.info'
    static jAdamsShelf = 'john.adamsu' // @shelfunit.info'
    static jackShelf = 'oneillu' // @shelfunit.info'
    static gwGroovy  = 'george.washingtonu' // @groovy-is-groovy.org'
    static jaGroovy  = 'john.adamsu' // @groovy-is-groovy.org'
    static jackGroovy = 'oneillu' // @groovy-is-groovy.org'

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
        userCommand = new USERCommand( sql )
    }     // run before the first feature method
    
    def cleanupSpec() {
        sql.execute "DELETE FROM email_user where username in ('george.washingtonu', 'john.adamsu', 'oneillu')"
        sql.close()
    }   // run after the last feature method
   
    def addUsers() {
        def numIterations = 10000
        def salt = 'you say your password tastes like chicken? Add salt!'
        def atx512
        atx512 = new Sha512Hash( 'somePassword', 'george.washingtonu', numIterations )
        def params = [ 'george.washingtonu', atx512.toBase64(), 'SHA-512', numIterations, 'George', 'Washington', 0 ]
        sql.execute 'insert into  email_user( username, password_hash, password_algo, iterations, first_name, last_name, version ) values ( ?, ?, ?, ?, ?, ?, ? )', params
        
        atx512 = new Sha512Hash( 'somePassword', 'john.adamsu', numIterations )
        params = [ 'john.adamsu', atx512.toBase64(), 'SHA-512', numIterations, 'John', 'Adams', 0 ]
        sql.execute 'insert into  email_user( username, password_hash, password_algo, iterations, first_name, last_name, version ) values ( ?, ?, ?, ?, ?, ?, ? )', params
        
        atx512 = new Sha512Hash( 'somePassword', 'oneillu', numIterations )
        params = [ 'oneillu', atx512.toBase64(), 'SHA-512', numIterations, 'Jack', "O'Neill", 0 ]
        sql.execute 'insert into  email_user( username, password_hash, password_algo, iterations, first_name, last_name, version ) values ( ?, ?, ?, ?, ?, ?, ? )', params
        // sql.commit()
    }


	/*00:44:46.135 [Test worker] INFO  i.s.postoffice.command.USERCommand - here is resultMap: [resultString:+OK george.washingtonu is a valid mailbox, bufferMap:[state:AUTHORIZATION, userInfo:[userid:199, username:george.washingtonu, password_hash:q84tQlFbTCkx/l5xyD4cvM81kCIRe33kt1ilPdT5E81k0WUVy73a5v2tQeuGGGDjfpEdBQj2Fuq+McYHE8+1Ig==, password_algo:SHA-512, iterations:10000, first_name:George, last_name:Washington, version:0]], prevCommandSet:[EHLO, MAIL]]

	*/
	@Unroll( "#inputAddress gives #value" )
	def "#inputAddress gives #value"() {
	    def resultMap
	    def resultString

        when:
            resultMap = userCommand.process( "USER ${inputAddress}", [] as Set,  [ state: 'AUTHORIZATION' ] )
        then:
            resultMap.resultString == value
        where:
            inputAddress    | value    
            gwShelf         | "+OK ${gwShelf} is a valid mailbox"
            jAdamsShelf     | "+OK ${jAdamsShelf} is a valid mailbox"
            jackShelf       | "+OK ${jackShelf} is a valid mailbox"
            gwGroovy        | "+OK ${gwGroovy} is a valid mailbox"
            jaGroovy        | "+OK ${jaGroovy} is a valid mailbox"
            jackGroovy      | "+OK ${jackGroovy} is a valid mailbox"
	}
	
	@Unroll( "#someState gives #value" )
	def "#someState gives #value"() {
	    def resultMap
	    def resultString

        when:
            resultMap = userCommand.process( "USER some.user", [] as Set, [ state: "${someState}" ] )
        then:
            println "command was EHLO, resultString is ${resultMap.resultString}"
            resultMap.resultString == value
            resultMap.bufferMap.state == finalState
        where:
            someState       | value                             | finalState
            'AUTHORIZATION' | "-ERR No such user some.user"     | 'AUTHORIZATION'
            'TRANSACTION'   | "-ERR Not in AUTHORIZATION state" | 'TRANSACTION'
            'UPDATE'        | "-ERR Not in AUTHORIZATION state" | 'UPDATE'
	}

	@Unroll( "non-existent user #inputAddress gives #value" )
	def "non-existent user #inputAddress gives #value"() {
	    def resultMap
	    def resultString

        when:
            resultMap = userCommand.process( "USER ${inputAddress}", [] as Set, [ state: 'AUTHORIZATION' ] )
        then:
            println "command was EHLO, resultString is ${resultMap.resultString}"
            resultMap.resultString == value
        where:
            inputAddress    | value                             | state
            'mkyong'        | "-ERR No such user mkyong"        | 'AUTHORIZATION'
            'mkyong-100'    | "-ERR No such user mkyong-100"    | 'AUTHORIZATION' 
            'mkyong.100'    | "-ERR No such user mkyong.100"    | 'AUTHORIZATION'
            'mkyong111'     | "-ERR No such user mkyong111"     | 'AUTHORIZATION'
            'mkyong+100'    | "-ERR No such user mkyong+100"    | 'AUTHORIZATION'
            'howTuser'      | "-ERR No such user howTuser"      | 'AUTHORIZATION'
            'user'          | "-ERR No such user user"          | 'AUTHORIZATION'
            'user1'         | "-ERR No such user user1"         | 'AUTHORIZATION'
            'user.name'     | "-ERR No such user user.name"     | 'AUTHORIZATION'
            'user_name'     | "-ERR No such user user_name"     | 'AUTHORIZATION'
            'user-name'     | "-ERR No such user user-name"     | 'AUTHORIZATION'
            'john.adams'    | "-ERR No such user john.adams"    | 'AUTHORIZATION'
            'oneill'        | "-ERR No such user oneill"        | 'AUTHORIZATION'
            'george.washington'  | "-ERR No such user george.washington"  | 'AUTHORIZATION' 
	}
	
} // line 152

