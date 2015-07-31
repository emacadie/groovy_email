package info.shelfunit.socket.command

import spock.lang.Specification
import spock.lang.Unroll

import org.junit.Rule
import org.junit.rules.TestName

import info.shelfunit.mail.MailRunner

import groovy.sql.Sql

import org.apache.shiro.crypto.hash.Sha512Hash

class RCPTCommandSpec extends Specification {
    
    def crlf = "\r\n"
    static domainList = [ 'shelfunit.info', 'groovy-is-groovy.org' ]
    static sql
    static rcptCommand

    @Rule 
    TestName name = new TestName()
    
    def setup() {
        println "\n--- Starting test ${name.methodName}"
    }          // run before every feature method
    def cleanup() {}        // run after every feature method
    
    def setupSpec() {
        MailRunner.runMetaProgramming()
        def db = [ url: "jdbc:postgresql://${System.properties[ 'host_and_port' ]}/${System.properties[ 'dbname' ]}",
        user: System.properties[ 'dbuser' ], password: System.properties[ 'dbpassword' ], driver: 'org.postgresql.Driver' ]
        sql = Sql.newInstance( db.url, db.user, db.password, db.driver )
        this.addUsers()
        rcptCommand = new RCPTCommand( sql, domainList )
    }     // run before the first feature method
    
    def cleanupSpec() {
        sql.execute "DELETE FROM email_user"
        sql.close()
    }   // run after the last feature method
   
    def addUsers() {
        def numIterations = 10000
        def salt = 'you say your password tastes like chicken? Add salt!'
        def atx512 = new Sha512Hash( 'somePassword', salt, 1000000 )
        def params = [ 'george.washington', atx512.toBase64(), 'SHA-512', numIterations, 'George', 'Washington', 0 ]
        sql.execute 'insert into  email_user( username, password_hash, password_algo, iterations, first_name, last_name, version ) values ( ?, ?, ?, ?, ?, ?, ? )', params
        
        params = [ 'john.adams', atx512.toBase64(), 'SHA-512', numIterations, 'John', 'Adams', 0 ]
        sql.execute 'insert into  email_user( username, password_hash, password_algo, iterations, first_name, last_name, version ) values ( ?, ?, ?, ?, ?, ?, ? )', params
        
        params = [ 'oneill', atx512.toBase64(), 'SHA-512', numIterations, 'Jack', "O'Neill", 0 ]
        sql.execute 'insert into  email_user( username, password_hash, password_algo, iterations, first_name, last_name, version ) values ( ?, ?, ?, ?, ?, ?, ? )', params
        // sql.commit()
    }
    
	def "test handling wrong command"() {
	    
	    when:
	        def resultMap = rcptCommand.process( "MAIL FROM:<oneill@stargate.mil>", [ 'RCPT' ] as Set, [ forwardPath:  'alexander@shelfunit.info' ] )
	        def mailResponse = resultMap.resultString + crlf 
	    then:
	        mailResponse == "501 Command not in proper form\r\n"
	        resultMap.prevCommandSet == [ "RCPT" ] as Set
	}

	@Unroll( "#inputAddress gives #value" )
	def "#inputAddress gives #value"() {
	    def resultMap
	    def resultString

        when:
            resultMap = rcptCommand.process( "RCPT TO:<${inputAddress}>", [ 'EHLO', 'MAIL' ] as Set, [ forwardPath:  'alexander@shelfunit.info' ] )
        then:
            println "command was EHLO, resultString is ${resultMap.resultString}"
            resultMap.resultString == value
            resultMap.prevCommandSet == [ 'EHLO', 'MAIL', 'RCPT' ] as Set
        where:
            inputAddress                | value    
            'george.washington@shelfunit.info'  | "250 OK"
            'john.adams@shelfunit.info' | '250 OK'
            'oneill@shelfunit.info'     | '250 OK'
            'george.washington@groovy-is-groovy.org'  | "250 OK"
            'john.adams@groovy-is-groovy.org' | '250 OK'
            'oneill@groovy-is-groovy.org'     | '250 OK'
	}
	
	@Unroll( "#inputAddress with RCPT in previous command list does not add another RCPT and gives #value" )
	def "#inputAddress with RCPT in previous command list does not add another RCPT and gives #value"() {
	    def resultMap
	    def resultString

        when:
            resultMap = rcptCommand.process( "RCPT TO:<${inputAddress}>", [ 'EHLO', 'MAIL', 'RCPT' ] as Set, [ forwardPath:  'alexander@shelfunit.info' ] )
        then:
            println "command was EHLO, resultString is ${resultMap.resultString}"
            resultMap.resultString == value
            resultMap.prevCommandSet == [ 'EHLO', 'MAIL', 'RCPT' ] as Set
        where:
            inputAddress                | value    
            'george.washington@shelfunit.info'  | "250 OK"
            'john.adams@shelfunit.info' | '250 OK'
            'oneill@shelfunit.info'     | '250 OK'
            'george.washington@groovy-is-groovy.org'  | "250 OK"
            'john.adams@groovy-is-groovy.org' | '250 OK'
            'oneill@groovy-is-groovy.org'     | '250 OK'
	}
	
	@Unroll( "#inputAddress with prev command sequence gives #value" )
	def "#inputAddress with prev command sequence gives #value"() {
	    def resultMap
	    def resultString

        when:
            resultMap = rcptCommand.process( "RCPT TO:<${inputAddress}>", prevCommandSet, [ forwardPath:  ['alexander@shelfunit.info'] ] )
        then:
            println "command was EHLO, resultString is ${resultMap.resultString}"
            resultMap.resultString == value
            resultMap.prevCommandSet == [ 'EHLO', 'MAIL', 'RCPT' ] as Set
            resultMap.bufferMap.forwardPath == forwardList
        where:
            inputAddress                | prevCommandSet | value | forwardList    
            'george.washington@shelfunit.info'  | [ 'EHLO', 'MAIL', 'RCPT' ] as Set | "250 OK" | ['alexander@shelfunit.info', 'george.washington@shelfunit.info' ]
            'john.adams@shelfunit.info' | [ 'EHLO', 'MAIL', 'RCPT' ] as Set | "250 OK" | ['alexander@shelfunit.info', 'john.adams@shelfunit.info' ]
            'oneill@shelfunit.info'     | [ 'EHLO', 'MAIL', 'RCPT' ] as Set | "250 OK" | ['alexander@shelfunit.info', 'oneill@shelfunit.info' ]
            'george.washington@groovy-is-groovy.org'  | [ 'EHLO', 'MAIL', 'RCPT' ] as Set | "250 OK" | ['alexander@shelfunit.info', 'george.washington@groovy-is-groovy.org' ]
            'john.adams@groovy-is-groovy.org' | [ 'EHLO', 'MAIL', 'RCPT' ] as Set | "250 OK" | ['alexander@shelfunit.info', 'john.adams@groovy-is-groovy.org' ]
            'oneill@groovy-is-groovy.org'     | [ 'EHLO', 'MAIL', 'RCPT' ] as Set | "250 OK" | ['alexander@shelfunit.info', 'oneill@groovy-is-groovy.org' ]
            'george.washington@shelfunit.info'  | [ 'EHLO', 'MAIL' ] as Set | "250 OK" | ['alexander@shelfunit.info', 'george.washington@shelfunit.info' ]
            'john.adams@shelfunit.info' | [ 'EHLO', 'MAIL' ] as Set | "250 OK" | ['alexander@shelfunit.info', 'john.adams@shelfunit.info' ]
            'oneill@shelfunit.info'     | [ 'EHLO', 'MAIL' ] as Set | "250 OK" | ['alexander@shelfunit.info', 'oneill@shelfunit.info' ]
            'george.washington@groovy-is-groovy.org'  | [ 'EHLO', 'MAIL' ] as Set | "250 OK" | ['alexander@shelfunit.info', 'george.washington@groovy-is-groovy.org' ]
            'john.adams@groovy-is-groovy.org' | [ 'EHLO', 'MAIL' ] as Set | "250 OK" | ['alexander@shelfunit.info', 'john.adams@groovy-is-groovy.org' ]
            'oneill@groovy-is-groovy.org'     | [ 'EHLO', 'MAIL' ] as Set | "250 OK" | ['alexander@shelfunit.info', 'oneill@groovy-is-groovy.org' ]
	}
	
	@Unroll( "#inputAddress with wrong prev command sequence gives #value" )
	def "#inputAddress with wrong prev command sequence gives #value"() {
	    def resultMap
	    def resultString

        when:
            resultMap = rcptCommand.process( "RCPT TO:<${inputAddress}>", prevCommandSet, [:] )
        then:
            println "command was EHLO, resultString is ${resultMap.resultString}"
            resultMap.resultString == value
            resultMap.prevCommandSet == prevCommandSet
        where:
            inputAddress                | prevCommandSet | value    
            'george.washington@shelfunit.info'  | [ 'EHLO' ] as Set | "503 Bad sequence of commands"
            'john.adams@shelfunit.info' | [ 'RSET' ] as Set | "503 Bad sequence of commands"

	}

	@Unroll( "#inputAddress with wrong domain gives #value" )
	def "#inputAddress with wrong domain gives #value"() {
	    def resultMap
	    def resultString

        when:
            resultMap = rcptCommand.process( "RCPT TO:<${inputAddress}>", [ 'EHLO', 'MAIL' ] as Set, [ forwardPath:  ['alexander@shelfunit.info'] ] )
        then:
            println "command was EHLO, resultString is ${resultMap.resultString}"
            resultMap.resultString == value
            resultMap.prevCommandSet == [ 'EHLO', 'MAIL' ] as Set
            resultMap.bufferMap.forwardPath == forwardList
        where:
            inputAddress                | value                 | forwardList
            'mkyong@yahoo.com'          | "550 No such user"    | [ 'alexander@shelfunit.info' ]
            'mkyong-100@yahoo.com'      | "550 No such user"    | [ 'alexander@shelfunit.info' ] 
            'mkyong.100@yahoo.com'      | "550 No such user"    | [ 'alexander@shelfunit.info' ]
            'mkyong111@mkyong.com'      | "550 No such user"    | [ 'alexander@shelfunit.info' ]
            'mkyong-100@mkyong.net'     | "550 No such user"    | [ 'alexander@shelfunit.info' ]
            'mkyong.100@mkyong.com.au'  | "550 No such user"    | [ 'alexander@shelfunit.info' ]
            'mkyong@1.com'              | "550 No such user"    | [ 'alexander@shelfunit.info' ]
            'mkyong@gmail.com.com'      | "550 No such user"    | [ 'alexander@shelfunit.info' ]
            'mkyong+100@gmail.com'      | "550 No such user"    | [ 'alexander@shelfunit.info' ]
            'mkyong-100@yahoo-test.com' | "550 No such user"    | [ 'alexander@shelfunit.info' ]
            'howTuser@domain.com'       | "550 No such user"    | [ 'alexander@shelfunit.info' ]
            'user@domain.co.in'         | "550 No such user"    | [ 'alexander@shelfunit.info' ]
            'user1@domain.com'          | "550 No such user"    | [ 'alexander@shelfunit.info' ]
            'user.name@domain.com'      | "550 No such user"    | [ 'alexander@shelfunit.info' ]
            'user_name@domain.co.in'    | "550 No such user"    | [ 'alexander@shelfunit.info' ]
            'user-name@domain.co.in'    | "550 No such user"    | [ 'alexander@shelfunit.info' ]
            // 'user@domaincom'            | "550 No such user" 
            'user@domain.com'           | "550 No such user"    | [ 'alexander@shelfunit.info' ]
            'user@domain.co.in'         | "550 No such user"    | [ 'alexander@shelfunit.info' ]
            'user.name@domain.com'      | "550 No such user"    | [ 'alexander@shelfunit.info' ]
            // "user'name@domain.co.in"    | "550 No such user"
            'user@domain.com'           | "550 No such user"    | [ 'alexander@shelfunit.info' ]
            'user@domain.co.in'         | "550 No such user"    | [ 'alexander@shelfunit.info' ]
            'user.name@domain.com'      | "550 No such user"    | [ 'alexander@shelfunit.info' ]
            'user_name@domain.com'      | "550 No such user"    | [ 'alexander@shelfunit.info' ]
            'username@yahoo.corporate.in'       | "550 No such user" | [ 'alexander@shelfunit.info' ]    
            'george.washington@mtvernon.co'  | "550 No such user"  | [ 'alexander@shelfunit.info' ] 
            'john.adams@his-rotundity.org' | "550 No such user" | [ 'alexander@shelfunit.info' ]
            'oneill@stargate.mil'       | "550 No such user"    | [ 'alexander@shelfunit.info' ]
	}
	
	@Unroll( "invalid address #inputAddress gives #value" )
	def "invalid address #inputAddress gives #value"() {
	    def resultMap
	    def resultString

        when:
            resultMap = rcptCommand.process( "RCPT TO:<${inputAddress}>", [ 'EHLO', 'MAIL' ] as Set, [ forwardPath:  'alexander@shelfunit.info' ] )
        then:
            println "command was EHLO, resultString is ${resultMap.resultString}"
            resultMap.resultString == value
            resultMap.bufferMap?.reversePath == resultAddress
            resultMap.prevCommandSet == [ 'EHLO', 'MAIL' ] as Set
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
            '@yahoo.com'                | "501 Command not in proper form"  | null 
            '.username@yahoo.com'       | "501 Command not in proper form"  | null 
            'username@yahoo.com.'       | "501 Command not in proper form"  | null 
            'username@yahoo..com'       | "501 Command not in proper form"  | null 
            '.username@yahoo.com'       | "501 Command not in proper form"  | null 
            'username@yahoo.com.'       | "501 Command not in proper form"  | null 
            'username@yahoo..com'       | "501 Command not in proper form"  | null 
            'username@yahoo.c'          | "501 Command not in proper form"  | null 
            'username@yahoo.corporate'  | "501 Command not in proper form"  | null 
	}
	
	def "test happy path"() {
	    when:
	        def resultMap = rcptCommand.process( "RCPT TO:<oneill@shelfunit.info>", [ 'EHLO', 'MAIL' ] as Set, [ forwardPath:  ['alexander@shelfunit.info'] ] )
	        def mailResponse = resultMap.resultString + crlf 
	        def bMap = resultMap.bufferMap
	    then:
	        mailResponse == "250 OK\r\n"
	        resultMap.prevCommandSet == [ "EHLO", "MAIL", "RCPT" ] as Set
	        bMap.forwardPath == [ 'alexander@shelfunit.info', 'oneill@shelfunit.info' ]
	}

}

