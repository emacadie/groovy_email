package info.shelfunit.postoffice.command

import spock.lang.Specification
import spock.lang.Unroll

import org.junit.Rule
import org.junit.rules.TestName

import info.shelfunit.mail.meta.MetaProgrammer
import info.shelfunit.mail.ConfigHolder
import static info.shelfunit.mail.GETestUtils.addUser
import static info.shelfunit.mail.GETestUtils.getRandomString

import groovy.util.logging.Slf4j 

@Slf4j
class USERCommandSpec extends Specification {
    
    def crlf = "\r\n"
    
    static domainList = [ 'shelfunit.info', 'groovy-is-groovy.org' ]
    static sql
    static userCommand
    static rString     = getRandomString( 12 )
    static gwShelf     = 'gw' + rString // @shelfunit.info'
    static jAdamsShelf = 'ja' + rString // @shelfunit.info'
    static jackShelf   = 'on' + rString // @shelfunit.info'
    static gwGroovy    = 'gw' + rString // @groovy-is-groovy.org'
    static jaGroovy    = 'ja' + rString // @groovy-is-groovy.org'
    static jackGroovy  = 'on' + rString // @groovy-is-groovy.org'

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
        userCommand = new USERCommand( sql )
    }     // run before the first feature method
    
    def cleanupSpec() {
        sql.execute "DELETE FROM email_user where username in ( ?, ?, ?)", [ gwShelf, jAdamsShelf, jackShelf ]
        sql.close()
    }   // run after the last feature method
   
    def addUsers() {
        addUser( sql, 'George', 'Washington', gwShelf, 'somePassword' )
        addUser( sql, 'John', 'Adams', jAdamsShelf, 'somePassword' )
        addUser( sql, 'Jack', "O'Neill", jackShelf, 'somePassword' )
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
            inputAddress | value    
            gwShelf      | "+OK ${gwShelf} is a valid mailbox"
            jAdamsShelf  | "+OK ${jAdamsShelf} is a valid mailbox"
            jackShelf    | "+OK ${jackShelf} is a valid mailbox"
            gwGroovy     | "+OK ${gwGroovy} is a valid mailbox"
            jaGroovy     | "+OK ${jaGroovy} is a valid mailbox"
            jackGroovy   | "+OK ${jackGroovy} is a valid mailbox"
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
            'john.adamsuu'  | "-ERR No such user john.adamsuu"  | 'AUTHORIZATION'
            'oneilluu'      | "-ERR No such user oneilluu"      | 'AUTHORIZATION'
            'grg.wshngtnuu' | "-ERR No such user grg.wshngtnuu" | 'AUTHORIZATION' 
	}
	
} // line 152, 148

