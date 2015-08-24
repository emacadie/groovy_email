package info.shelfunit.postoffice.command

import spock.lang.Specification
import spock.lang.Unroll

import org.junit.Rule
import org.junit.rules.TestName

import info.shelfunit.mail.MetaProgrammer
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
    static hamilton = 'alexander' // @shelfunit.info'
    static gwShelf  = 'george.washingtonp' // @shelfunit.info'
    static jAdamsShelf = 'john.adamsp' // @shelfunit.info'
    static jackShell = 'oneillp' // @shelfunit.info'
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
        this.addUsers()
        userCommand = new USERCommand( sql )
    }     // run before the first feature method
    
    def cleanupSpec() {
        sql.execute "DELETE FROM email_user where username in ('george.washingtonp', 'john.adamsp', 'oneillp')"
        sql.close()
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
    /*
	def "test handling wrong command"() {
	    
	    when:
	        def resultMap = userCommand.process( "MAIL FROM:<oneill@stargate.mil>", [ 'RCPT' ] as Set, [ forwardPath:  [ hamilton ] ] )
	        def mailResponse = resultMap.resultString + crlf 
	    then:
	        mailResponse == "501 Command not in proper form\r\n"
	        resultMap.prevCommandSet == [ "RCPT" ] as Set
	}
	*/

	/*
	@Unroll( "#inputAddress gives #value" )
	def "#inputAddress gives #value"() {
	    def resultMap
	    def resultString

        when:
            resultMap = userCommand.process( "RCPT TO:<${inputAddress}>", resultSetEM, [ forwardPath:  [ hamilton ] ] )
        then:
            println "command was EHLO, resultString is ${resultMap.resultString}"
            resultMap.resultString == value
            resultMap.prevCommandSet == resultSetEMR
        where:
            inputAddress    | value    
            gwShelf         | "250 OK"
            jAdamsShelf     | '250 OK'
            jackShell       | '250 OK'
            gwGroovy        | "250 OK"
            jaGroovy        | '250 OK'
            jackGroovy      | '250 OK'
	}
	*/
	/*
	@Unroll( "#inputAddress with RCPT in previous command list does not add another RCPT and gives #value" )
	def "#inputAddress with RCPT in previous command list does not add another RCPT and gives #value"() {
	    def resultMap
	    def resultString

        when:
            resultMap = userCommand.process( "RCPT TO:<${inputAddress}>", resultSetEMR, [ forwardPath:  hamilton ] )
        then:
            println "command was EHLO, resultString is ${resultMap.resultString}"
            resultMap.resultString == value
            resultMap.prevCommandSet == resultSetEMR
        where:
            inputAddress    | value    
            gwShelf         | "250 OK"
            jAdamsShelf     | '250 OK'
            jackShell       | '250 OK'
            gwGroovy        | "250 OK"
            jaGroovy        | '250 OK'
            jackGroovy      | '250 OK'
	}
	*/
	/*
	@Unroll( "#inputAddress with prev command sequence gives #value" )
	def "#inputAddress with prev command sequence gives #value"() {
	    def resultMap
	    def resultString
	    
        when:
            resultMap = userCommand.process( "RCPT TO:<${inputAddress}>", prevCommandSet, [ forwardPath: [ hamilton ] ] )
        then:
            println "command was EHLO, resultString is ${resultMap.resultString}"
            resultMap.resultString == value
            resultMap.prevCommandSet == resultSetEMR
            resultMap.bufferMap.forwardPath == forwardList
        where:
            inputAddress    | prevCommandSet    | value     | forwardList    
            gwShelf         | resultSetEMR      | "250 OK"  | [ hamilton, gwShelf ]
            jAdamsShelf     | resultSetEMR      | "250 OK"  | [ hamilton, jAdamsShelf ]
            jackShell       | resultSetEMR      | "250 OK"  | [ hamilton, jackShell ]
            gwGroovy        | resultSetEMR      | "250 OK"  | [ hamilton, gwGroovy ]
            jaGroovy        | resultSetEMR      | "250 OK"  | [ hamilton, jaGroovy ]
            jackGroovy      | resultSetEMR      | "250 OK"  | [ hamilton, jackGroovy ]
            gwShelf         | resultSetEM       | "250 OK"  | [ hamilton, gwShelf ]
            jAdamsShelf     | resultSetEM       | "250 OK"  | [ hamilton, jAdamsShelf ]
            jackShell       | resultSetEM       | "250 OK"  | [ hamilton, jackShell ]
            gwGroovy        | resultSetEM       | "250 OK"  | [ hamilton, gwGroovy ]
            jaGroovy        | resultSetEM       | "250 OK"  | [ hamilton, jaGroovy ]
            jackGroovy      | resultSetEM       | "250 OK"  | [ hamilton, jackGroovy ]
	}
	*/
	/*
	@Unroll( "#inputAddress with wrong prev command sequence gives #value" )
	def "#inputAddress with wrong prev command sequence gives #value"() {
	    def resultMap
	    def resultString

        when:
            resultMap = userCommand.process( "RCPT TO:<${inputAddress}>", prevCommandSet, [:] )
        then:
            println "command was EHLO, resultString is ${resultMap.resultString}"
            resultMap.resultString == value
            resultMap.prevCommandSet == prevCommandSet
        where:
            inputAddress    | prevCommandSet | value    
            gwShelf         | [ 'EHLO' ] as Set | "503 Bad sequence of commands"
            jAdamsShelf     | [ 'RSET' ] as Set | "503 Bad sequence of commands"

	}
	*/

	@Unroll( "#inputAddress with wrong domain gives #value" )
	def "#inputAddress with wrong domain gives #value"() {
	    def resultMap
	    def resultString

        when:
            resultMap = userCommand.process( "USER ${inputAddress}", resultSetEM, [ state: 'AUTHORIZATION' ] )
        then:
            println "command was EHLO, resultString is ${resultMap.resultString}"
            resultMap.resultString == value
            resultMap.prevCommandSet == resultSetEM
            // resultMap.bufferMap.state == forwardList
        where:
            inputAddress                | value                 | state
            'mkyong'          | "-ERR No such user mkyong"    | 'AUTHORIZATION'
            'mkyong-100'      | "-ERR No such user mkyong-100"    | 'AUTHORIZATION' 
            'mkyong.100'      | "-ERR No such user mkyong.100"    | 'AUTHORIZATION'
            'mkyong111'      | "-ERR No such user mkyong111"    | 'AUTHORIZATION'
            'mkyong+100'      | "-ERR No such user mkyong+100"    | 'AUTHORIZATION'
            'howTuser'       | "-ERR No such user howTuser"    | 'AUTHORIZATION'
            'user'         | "-ERR No such user user"    | 'AUTHORIZATION'
            'user1'          | "-ERR No such user user1"    | 'AUTHORIZATION'
            'user.name'      | "-ERR No such user user.name"    | 'AUTHORIZATION'
            'user_name'    | "-ERR No such user user_name"    | 'AUTHORIZATION'
            'user-name'    | "-ERR No such user user-name"    | 'AUTHORIZATION'
            'george.washington'  | "-ERR No such user george.washington"  | 'AUTHORIZATION' 
            'john.adams' | "-ERR No such user john.adams" | 'AUTHORIZATION'
            'oneill'       | "-ERR No such user oneill"    | 'AUTHORIZATION'
            // 'user@domaincom'            | "-ERR No such user" 
            // "user'name@domain.co.in"    | "-ERR No such user"
	}
	
	/*
	@Unroll( "invalid address #inputAddress gives #value" )
	def "invalid address #inputAddress gives #value"() {
	    def resultMap
	    def resultString

        when:
            resultMap = userCommand.process( "RCPT TO:<${inputAddress}>", resultSetEM, [ forwardPath:  hamilton ] )
        then:
            println "command was EHLO, resultString is ${resultMap.resultString}"
            resultMap.resultString == value
            resultMap.bufferMap?.reversePath == resultAddress
            resultMap.prevCommandSet == resultSetEM
        where:
            inputAddress                | value                             | resultAddress
            'mkyong'                    | "501 Command not in proper form"  | null 
            'mkyong@.com.my'            | "501 Command not in proper form"  | null 
            'mkyong123@gmail.a'         | "501 Command not in proper form"  | null 
            'mkyong123@.com'            | "501 Command not in proper form"  | null 
            'mkyong123@.com.com'        | "501 Command not in proper form"  | null 
            '.mkyong@mkyong.com'        | "501 Command not in proper form"  | null 
            'mkyong()*@gmail.com'       | "501 Command not in proper form"  | null 
            'mkyong@%*.com'             | "501 Command not in proper form"  | null 
            'mkyong..2002@gmail.com'    | "501 Command not in proper form"  | null 
            'mkyong.@gmail.com'         | "501 Command not in proper form"  | null 
            'mkyong@mkyong@gmail.com'   | "501 Command not in proper form"  | null 
            'mkyong@gmail.com.1a'       | "501 Command not in proper form"  | null 
            ''                | "501 Command not in proper form"  | null 
            '.username'       | "501 Command not in proper form"  | null 
            'username.'       | "501 Command not in proper form"  | null 
            'username@yahoo..com'       | "501 Command not in proper form"  | null 
            '.username'       | "501 Command not in proper form"  | null 
            'username.'       | "501 Command not in proper form"  | null 
            'username@yahoo..com'       | "501 Command not in proper form"  | null 
            'username@yahoo.c'          | "501 Command not in proper form"  | null 
            'username@yahoo.corporate'  | "501 Command not in proper form"  | null 
	}
	*/
	/*
	def "test happy path"() {
	    when:
	        def resultMap = userCommand.process( "RCPT TO:<${jackShell}>", resultSetEM, [ forwardPath:  [ hamilton ] ] )
	        def mailResponse = resultMap.resultString + crlf 
	        def bMap = resultMap.bufferMap
	    then:
	        mailResponse == "250 OK\r\n"
	        resultMap.prevCommandSet == resultSetEMR
	        bMap.forwardPath == [ hamilton, jackShell ]
	}
	*/

}

