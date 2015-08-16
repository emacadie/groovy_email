package info.shelfunit.socket.command

import spock.lang.Specification
import spock.lang.Unroll

import org.junit.Rule
import org.junit.rules.TestName

import info.shelfunit.mail.MetaProgrammer

import groovy.sql.Sql

import org.apache.shiro.crypto.hash.Sha512Hash

class MSSGCommandSpec extends Specification {
    
    def crlf = "\r\n"
    static domainList = [ 'shelfunit2.info', 'groovy-is-groovy2.org' ]
    static sql
    static mssgCommand
    static hamilton = 'alexander2@shelfunit2.info'
    static gwShelf  = 'george.washington2@shelfunit2.info'
    static jAdamsShelf = 'john.adams2@shelfunit2.info'
    static jackShell = 'oneill2@shelfunit2.info'
    static gwGroovy  = 'george.washington2@groovy-is-groovy2.org'
    static jaGroovy  = 'john.adams2@groovy-is-groovy2.org'
    static jackGroovy = 'oneill2@groovy-is-groovy2.org'
    static resultSetEMR = [ 'EHLO', 'MAIL', 'RCPT' ] as Set
    static resultSetEM = [ 'EHLO', 'MAIL' ] as Set 

    @Rule 
    TestName name = new TestName()
    
    def setup() {
        println "\n--- Starting test ${name.methodName}"
    }          // run before every feature method
    def cleanup() {}        // run after every feature method
    
    def setupSpec() {
        MetaProgrammer.runMetaProgramming()
        def db = [ url: "jdbc:postgresql://${System.properties[ 'host_and_port' ]}/${System.properties[ 'dbname' ]}",
        user: System.properties[ 'dbuser' ], password: System.properties[ 'dbpassword' ], driver: 'org.postgresql.Driver' ]
        sql = Sql.newInstance( db.url, db.user, db.password, db.driver )
        this.addUsers()
        mssgCommand = new MSSGCommand( sql, domainList )
    }     // run before the first feature method
    
    def cleanupSpec() {
        sql.execute "DELETE FROM email_user where username in ('george.washington', 'john.adams', 'oneill')"
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
	    def bufferMapArg = [ forwardPath:[ 'alexander@shelfunit.info', 'george.washington@shelfunit.info' ], reversePath: 'oneill@stargate.mil' ]
        def prevCommandSetArg = [ 'EHLO', 'MAIL', 'RCPT' ] as Set
	    when:
	        def resultMap = mssgCommand.process( "The next meeting of the board of directors will be on Tuesday.\nJohn.", prevCommandSetArg, [ forwardPath:  [ hamilton ] ] )
	        def mailResponse = resultMap.resultString + crlf 
	    then:
	        mailResponse == "503 Bad sequence of commands\r\n"
	        resultMap.prevCommandSet == prevCommandSetArg
	}
	
	def "test handling a message"() {
	    def bufferMapArg = [ forwardPath:[ 'john.adams@shelfunit.info', 'george.washington@shelfunit.info' ], reversePath: 'oneill@stargate.mil' ]
	    def uuidSet = [] as Set
	    bufferMapArg.forwardPath.size().times() {
            uuidSet << UUID.randomUUID() // .toString()
        }
        def countResult
        def qList = []
        ( 1..uuidSet.size() ).each { qList << '?' }
        def qCString = qList.join( ',' )
        def sqlString = 'select count(*) from mail_store where id in (' + qCString + ')'
        when:
            countResult = sql.firstRow( sqlString, uuidSet as List )
        then:
            countResult.count == 0
            
        def theMessage = "The next meeting of the board of directors will be on Tuesday.\nJohn."
        def prevCommandSetArg = [ 'EHLO', 'MAIL', 'RCPT' ] as Set
	    when:
	        def mailResponse = mssgCommand.addMessageToDatabase( theMessage, bufferMapArg, uuidSet ) 
	        countResult = sql.firstRow( sqlString, uuidSet as List )
	    then:
	        mailResponse == "250 OK"
	        countResult.count == uuidSet.size()
	}
	
	def "test getting the data"() {
	    sql.eachRow( 'select * from mail_store' ) { mailItem ->
	        println mailItem.id.toString()
            println "mailItem.text_body is a ${mailItem.text_body.getClass().getName()}"
            println "mailItem.text_body: ${mailItem.text_body}"
            println "mailItem.username: ${mailItem.username}\n-------------------------------------------------"
        }
	    expect:
	        5 ==5
	}
	
	/*
	@Unroll( "#inputAddress gives #value" )
	def "#inputAddress gives #value"() {
	    def resultMap
	    def resultString

        when:
            resultMap = mssgCommand.process( "RCPT TO:<${inputAddress}>", resultSetEM, [ forwardPath:  [ hamilton ] ] )
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
            resultMap = mssgCommand.process( "RCPT TO:<${inputAddress}>", resultSetEMR, [ forwardPath:  hamilton ] )
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
            resultMap = mssgCommand.process( "RCPT TO:<${inputAddress}>", prevCommandSet, [ forwardPath: [ hamilton ] ] )
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
            resultMap = mssgCommand.process( "RCPT TO:<${inputAddress}>", prevCommandSet, [:] )
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
	/*
	@Unroll( "#inputAddress with wrong domain gives #value" )
	def "#inputAddress with wrong domain gives #value"() {
	    def resultMap
	    def resultString

        when:
            resultMap = mssgCommand.process( "RCPT TO:<${inputAddress}>", resultSetEM, [ forwardPath:  [ hamilton ] ] )
        then:
            println "command was EHLO, resultString is ${resultMap.resultString}"
            resultMap.resultString == value
            resultMap.prevCommandSet == resultSetEM
            resultMap.bufferMap.forwardPath == forwardList
        where:
            inputAddress                | value                 | forwardList
            'mkyong@yahoo.com'          | "550 No such user"    | [ hamilton ]
            'mkyong-100@yahoo.com'      | "550 No such user"    | [ hamilton ] 
            'mkyong.100@yahoo.com'      | "550 No such user"    | [ hamilton ]
            'mkyong111@mkyong.com'      | "550 No such user"    | [ hamilton ]
            'mkyong-100@mkyong.net'     | "550 No such user"    | [ hamilton ]
            'mkyong.100@mkyong.com.au'  | "550 No such user"    | [ hamilton ]
            'mkyong@1.com'              | "550 No such user"    | [ hamilton ]
            'mkyong@gmail.com.com'      | "550 No such user"    | [ hamilton ]
            'mkyong+100@gmail.com'      | "550 No such user"    | [ hamilton ]
            'mkyong-100@yahoo-test.com' | "550 No such user"    | [ hamilton ]
            'howTuser@domain.com'       | "550 No such user"    | [ hamilton ]
            'user@domain.co.in'         | "550 No such user"    | [ hamilton ]
            'user1@domain.com'          | "550 No such user"    | [ hamilton ]
            'user.name@domain.com'      | "550 No such user"    | [ hamilton ]
            'user_name@domain.co.in'    | "550 No such user"    | [ hamilton ]
            'user-name@domain.co.in'    | "550 No such user"    | [ hamilton ]
            'user@domain.com'           | "550 No such user"    | [ hamilton ]
            'user@domain.co.in'         | "550 No such user"    | [ hamilton ]
            'user.name@domain.com'      | "550 No such user"    | [ hamilton ]
            'user@domain.com'           | "550 No such user"    | [ hamilton ]
            'user@domain.co.in'         | "550 No such user"    | [ hamilton ]
            'user.name@domain.com'      | "550 No such user"    | [ hamilton ]
            'user_name@domain.com'      | "550 No such user"    | [ hamilton ]
            'username@yahoo.corporate.in'       | "550 No such user" | [ hamilton ]    
            'george.washington@mtvernon.co'  | "550 No such user"  | [ hamilton ] 
            'john.adams@his-rotundity.org' | "550 No such user" | [ hamilton ]
            'oneill@stargate.mil'       | "550 No such user"    | [ hamilton ]
            // 'user@domaincom'            | "550 No such user" 
            // "user'name@domain.co.in"    | "550 No such user"
	}
	*/
	/*
	@Unroll( "invalid address #inputAddress gives #value" )
	def "invalid address #inputAddress gives #value"() {
	    def resultMap
	    def resultString

        when:
            resultMap = mssgCommand.process( "RCPT TO:<${inputAddress}>", resultSetEM, [ forwardPath:  hamilton ] )
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
	*/
	/*
	def "test happy path"() {
	    when:
	        def resultMap = mssgCommand.process( "RCPT TO:<${jackShell}>", resultSetEM, [ forwardPath:  [ hamilton ] ] )
	        def mailResponse = resultMap.resultString + crlf 
	        def bMap = resultMap.bufferMap
	    then:
	        mailResponse == "250 OK\r\n"
	        resultMap.prevCommandSet == resultSetEMR
	        bMap.forwardPath == [ hamilton, jackShell ]
	}
	*/
}

