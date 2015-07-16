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
	        def resultMap = rcptCommand.process( "MAIL FROM:<oneill@stargate.mil>", [ 'RCPT' ], [:] )
	        def mailResponse = resultMap.resultString + crlf 
	    then:
	        mailResponse == "501 Command not in proper form\r\n"
	        resultMap.prevCommandList == [ "RCPT" ]
	}
	/*
	@Unroll( "#command should result in #mailResponse" )
	def "#command results in #mailResponse"() {
	    def resultMap
	    expect:
	        mailResponse == rcptCommand.process( "RCPT TO:<oneill@stargate.mil>", [ command ], [:] ).resultString
	    
	    where:
            command | mailResponse
            'MAIL'  | "503 Bad sequence of commands"
            'EXPN'  | "503 Bad sequence of commands"
            'VRFY'  | "503 Bad sequence of commands"
            'NOOP'  | "503 Bad sequence of commands"
            'RCPT'  | "503 Bad sequence of commands"
            'EHLO'  | "250 OK"
            'HELO'  | "250 OK"
            'RSET'  | "250 OK"
	}
	*/
	/*
	@Unroll( "#command gives #value with address #resultAddress" )
	def "#command gives #value with address #resultAddress"() {
	    def resultMap
	    def resultString

            when:
                resultMap = rcptCommand.process( "MAIL FROM:<oneill@stargate.mil>", [ command ], [:] )
            then:
                println "command was ${command}, resultString is ${resultMap.resultString}"
                resultMap.resultString == value
                resultMap.bufferMap?.reversePath == resultAddress
            where:
            command | value                          | resultAddress
            'EHLO'  | "250 OK"                       | 'oneill@stargate.mil'
            'HELO'  | "250 OK"                       | 'oneill@stargate.mil'
            'RSET'  | "250 OK"                       | 'oneill@stargate.mil'
            'MAIL'  | "503 Bad sequence of commands" | null
            'EXPN'  | "503 Bad sequence of commands" | null
            'VRFY'  | "503 Bad sequence of commands" | null
            'NOOP'  | "503 Bad sequence of commands" | null
            'RCPT'  | "503 Bad sequence of commands" | null
	}
	*/
	
	/*
	@Unroll( "#inputAddress gives #value with result Address the same" )
	def "#inputAddress gives #value with result Address the same"() {
	    def resultMap
	    def resultString

            when:
                resultMap = rcptCommand.process( "MAIL FROM:<${inputAddress}>", [ 'EHLO' ], [:] )
            then:
                println "command was EHLO, resultString is ${resultMap.resultString}"
                resultMap.resultString == value
                resultMap.bufferMap?.reversePath == inputAddress
                resultMap.prevCommandList == [ 'EHLO', 'MAIL' ]
            where:
            inputAddress                | value    
            'mkyong@yahoo.com'          | "250 OK" 
            'mkyong-100@yahoo.com'      | "250 OK" 
            'mkyong.100@yahoo.com'      | "250 OK" 
            'mkyong111@mkyong.com'      | "250 OK" 
            'mkyong-100@mkyong.net'     | "250 OK" 
            'mkyong.100@mkyong.com.au'  | "250 OK" 
            'mkyong@1.com'              | "250 OK" 
            'mkyong@gmail.com.com'      | "250 OK" 
            'mkyong+100@gmail.com'      | "250 OK" 
            'mkyong-100@yahoo-test.com' | "250 OK"
            'howTuser@domain.com'       | "250 OK" 
            'user@domain.co.in'         | "250 OK" 
            'user1@domain.com'          | "250 OK" 
            'user.name@domain.com'      | "250 OK" 
            'user_name@domain.co.in'    | "250 OK" 
            'user-name@domain.co.in'    | "250 OK" 
            // 'user@domaincom'            | "250 OK" 
            'user@domain.com'           | "250 OK" 
            'user@domain.co.in'         | "250 OK" 
            'user.name@domain.com'      | "250 OK"
            // "user'name@domain.co.in"    | "250 OK"
            'user@domain.com'           | "250 OK" 
            'user@domain.co.in'         | "250 OK" 
            'user.name@domain.com'      | "250 OK" 
            'user_name@domain.com'      | "250 OK" 
            'username@yahoo.corporate.in'   | "250 OK"
	}
	*/
	
	
	@Unroll( "invalid address #inputAddress gives #value" )
	def "invalid address #inputAddress gives #value"() {
	    def resultMap
	    def resultString

            when:
                resultMap = rcptCommand.process( "MAIL FROM:<${inputAddress}>", [ 'EHLO', 'MAIL' ], [:] )
            then:
                println "command was EHLO, resultString is ${resultMap.resultString}"
                resultMap.resultString == value
                resultMap.bufferMap?.reversePath == resultAddress
                resultMap.prevCommandList == [ 'EHLO', 'MAIL' ]
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
	        def resultMap = rcptCommand.process( "RCPT TO:<oneill@shelfunit.info>", [ 'EHLO', 'MAIL' ], [:] )
	        def mailResponse = resultMap.resultString + crlf 
	        def bMap = resultMap.bufferMap
	    then:
	        mailResponse == "250 OK\r\n"
	        resultMap.prevCommandList == [ "EHLO", "MAIL", "RCPT" ]
	        bMap.forwardPath == 'oneill@shelfunit.info'
	}

}

