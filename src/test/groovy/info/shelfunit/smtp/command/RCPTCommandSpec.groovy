package info.shelfunit.smtp.command

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
class RCPTCommandSpec extends Specification {
    
    def crlf = "\r\n"
    static domainList = [ 'shelfunit.info', 'groovy-is-groovy.org' ]
    static sql
    static rcptCommand
    static rString  = getRandomString()
    static hamilton = 'alexander@shelfunit.info'
    static jpJones  = 'jpjones@navy.mil'
    static gwString = 'gw' + rString
    static jaString = 'ja' + rString
    static joString = 'jo' + rString
    static gwShelf      = gwString + '@shelfunit.info'
    static jAdamsShelf  = jaString + '@shelfunit.info'
    static jackShell    = joString + '@shelfunit.info'
    static gwGroovy     = gwString + '@groovy-is-groovy.org'
    static jaGroovy     = jaString + '@groovy-is-groovy.org'
    static jackGroovy   = joString + '@groovy-is-groovy.org'
    static resultSetEMR = [ 'EHLO', 'MAIL', 'RCPT' ] as Set
    static resultSetEM  = [ 'EHLO', 'MAIL' ] as Set
    static response550 = "550 No such user"
    static response250 = "250 OK"
    static otherDomains = [ 'mkyong@yahoo.com', 'mkyong-100@yahoo.com',
	    'mkyong.100@yahoo.com', 'mkyong111@mkyong.com', 'mkyong-100@mkyong.net', 'mkyong.100@mkyong.com.au',
	    'mkyong@1.com', 'mkyong@gmail.com.com', 'mkyong+100@gmail.com', 'mkyong-100@yahoo-test.com',
	    'howTuser@domain.com', 'user@domain.co.in', 'user1@domain.com', 'user.name@domain.com',
	    'user_name@domain.co.in', 'user-name@domain.co.in', 'user@domain.com', 'user@domain.co.in',
	    'user.name@domain.com', 'user@domain.com', 'user@domain.co.in', 'user.name@domain.com', 'user_name@domain.com',
	    'username@yahoo.corporate.in', 'george.washingtonrcpt@mtvernon.co', 'john.adams@his-rotundity.org',
	    'oneill@stargate.mil' ]

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
        rcptCommand = new RCPTCommand( domainList )
    }     // run before the first feature method
    
    def cleanupSpec() {
        sql.execute "DELETE FROM email_user where username in ( ?, ?, ? )", [ gwString, jaString, joString ]
        sql.close()
    }   // run after the last feature method
   
    def addUsers() {
        addUser( sql, 'George', 'Washington', gwString, 'somePassword' )
        addUser( sql, 'John', 'Adams', jaString, 'somePassword' )
        addUser( sql, 'Jack', "O'Neill", joString, 'somePassword' )
    }
    
	def "test handling wrong command"() {
	    
	    when:
	        def resultMap = rcptCommand.process( "MAIL FROM:<oneill@stargate.mil>", [ 'RCPT' ] as Set, [ forwardPath:  [ hamilton ] ] )
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
            resultMap = rcptCommand.process( "RCPT TO:<${inputAddress}>", resultSetEM, [ forwardPath:  [ hamilton ], messageDirection: "${direction}" ] )
        then:
            println "command was EHLO, resultString is ${resultMap.resultString}"
            resultMap.resultString == value
            resultMap.prevCommandSet == resultSetEMR
        where:
            inputAddress | direction  | value     
            gwShelf      | 'inbound'  | response250  
            jAdamsShelf  | 'inbound'  | response250  
            jackShell    | 'inbound'  | response250  
            jackShell    | 'outbound' | response250  
            gwGroovy     | 'inbound'  | response250 
            jaGroovy     | 'inbound'  | response250 
            jackGroovy   | 'inbound'  | response250
            jackGroovy   | 'outbound' | response250
            jpJones      | 'outbound' | response250
            jpJones      | 'inbound'  | response550 
	} // 
	
	@Unroll( "#inputAddress with RCPT in previous command list does not add another RCPT and gives #value" )
	def "#inputAddress with RCPT in previous command list does not add another RCPT and gives #value"() {
	    def resultMap
	    def resultString

        when:
            resultMap = rcptCommand.process( "RCPT TO:<${inputAddress}>", resultSetEMR, [ forwardPath:  hamilton ] )
        then:
            println "command was EHLO, resultString is ${resultMap.resultString}"
            resultMap.resultString == value
            resultMap.prevCommandSet == resultSetEMR
        where:
            inputAddress    | value    
            gwShelf         | response250
            jAdamsShelf     | response250
            jackShell       | response250
            gwGroovy        | response250
            jaGroovy        | response250
            jackGroovy      | response250
	}
	
	@Unroll( "#inputAddress with prev command sequence gives #value" )
	def "#inputAddress with prev command sequence gives #value"() {
	    def resultMap
	    def resultString
	    
        when:
            resultMap = rcptCommand.process( "RCPT TO:<${inputAddress}>", prevCommandSet, [ forwardPath: [ hamilton ] ] )
        then:
            println "command was EHLO, resultString is ${resultMap.resultString}"
            resultMap.resultString == value
            resultMap.prevCommandSet == resultSetEMR
            resultMap.bufferMap.forwardPath == forwardList
        where:
            inputAddress    | prevCommandSet | value     | forwardList    
            gwShelf         | resultSetEMR   | response250  | [ hamilton, gwShelf ]
            jAdamsShelf     | resultSetEMR   | response250  | [ hamilton, jAdamsShelf ]
            jackShell       | resultSetEMR   | response250  | [ hamilton, jackShell ]
            gwGroovy        | resultSetEMR   | response250  | [ hamilton, gwGroovy ]
            jaGroovy        | resultSetEMR   | response250  | [ hamilton, jaGroovy ]
            jackGroovy      | resultSetEMR   | response250  | [ hamilton, jackGroovy ]
            gwShelf         | resultSetEM    | response250  | [ hamilton, gwShelf ]
            jAdamsShelf     | resultSetEM    | response250  | [ hamilton, jAdamsShelf ]
            jackShell       | resultSetEM    | response250  | [ hamilton, jackShell ]
            gwGroovy        | resultSetEM    | response250  | [ hamilton, gwGroovy ]
            jaGroovy        | resultSetEM    | response250  | [ hamilton, jaGroovy ]
            jackGroovy      | resultSetEM    | response250  | [ hamilton, jackGroovy ]
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
            inputAddress    | prevCommandSet | value    
            gwShelf         | [ 'EHLO' ] as Set | "503 Bad sequence of commands"
            jAdamsShelf     | [ 'RSET' ] as Set | "503 Bad sequence of commands"

	}

	@Unroll( "#inputAddress with wrong domain gives #value" )
	def "#inputAddress with wrong domain gives #value"() {
	    def resultMap
	    def resultString
	    /*
	    def otherDomains = [ 'mkyong@yahoo.com', 'mkyong-100@yahoo.com',
	    'mkyong.100@yahoo.com', 'mkyong111@mkyong.com', 'mkyong-100@mkyong.net', 'mkyong.100@mkyong.com.au',
	    'mkyong@1.com', 'mkyong@gmail.com.com', 'mkyong+100@gmail.com', 'mkyong-100@yahoo-test.com',
	    'howTuser@domain.com', 'user@domain.co.in', 'user1@domain.com', 'user.name@domain.com',
	    'user_name@domain.co.in', 'user-name@domain.co.in', 'user@domain.com', 'user@domain.co.in',
	    'user.name@domain.com', 'user@domain.com', 'user@domain.co.in', 'user.name@domain.com', 'user_name@domain.com',
	    'username@yahoo.corporate.in', 'george.washingtonrcpt@mtvernon.co', 'john.adams@his-rotundity.org',
	    'oneill@stargate.mil' ]
	    */

        when:
            
            resultMap = rcptCommand.process( "RCPT TO:<${inputAddress}>", resultSetEM, [ forwardPath:  [ hamilton ], messageDirection: "${direction}" ] )
        then:
            println "command was EHLO, resultString is ${resultMap.resultString}"
            resultMap.resultString == value
            resultMap.prevCommandSet == resultSetEM
            resultMap.bufferMap.forwardPath == forwardList
        where:
            inputAddress       | direction  | value       | forwardList
            otherDomains[ 0 ]  | 'inbound'  | response550 | [ hamilton ]
            otherDomains[ 1 ]  | 'inbound'  | response550 | [ hamilton ] 
            otherDomains[ 2 ]  | 'inbound'  | response550 | [ hamilton ]
            otherDomains[ 3 ]  | 'inbound'  | response550 | [ hamilton ]
            otherDomains[ 4 ]  | 'inbound'  | response550 | [ hamilton ]
            otherDomains[ 5 ]  | 'inbound'  | response550 | [ hamilton ]
            otherDomains[ 6 ]  | 'inbound'  | response550 | [ hamilton ]
            otherDomains[ 7 ]  | 'inbound'  | response550 | [ hamilton ]
            otherDomains[ 8 ]  | 'inbound'  | response550 | [ hamilton ]
            otherDomains[ 9 ]  | 'inbound'  | response550 | [ hamilton ]
            otherDomains[ 10 ] | 'inbound'  | response550 | [ hamilton ]
            otherDomains[ 11 ] | 'inbound'  | response550 | [ hamilton ]
            otherDomains[ 12 ] | 'inbound'  | response550 | [ hamilton ]
            otherDomains[ 13 ] | 'inbound'  | response550 | [ hamilton ]
            otherDomains[ 14 ] | 'inbound'  | response550 | [ hamilton ]
            otherDomains[ 15 ] | 'inbound'  | response550 | [ hamilton ]
            otherDomains[ 16 ] | 'inbound'  | response550 | [ hamilton ]
            otherDomains[ 17 ] | 'inbound'  | response550 | [ hamilton ]
            otherDomains[ 18 ] | 'inbound'  | response550 | [ hamilton ]
            otherDomains[ 19 ] | 'inbound'  | response550 | [ hamilton ]
            otherDomains[ 20 ] | 'inbound'  | response550 | [ hamilton ]
            otherDomains[ 21 ] | 'inbound'  | response550 | [ hamilton ]
            otherDomains[ 22 ] | 'inbound'  | response550 | [ hamilton ]
            otherDomains[ 23 ] | 'inbound'  | response550 | [ hamilton ]    
            otherDomains[ 24 ] | 'inbound'  | response550 | [ hamilton ] 
            otherDomains[ 25 ] | 'inbound'  | response550 | [ hamilton ]
            otherDomains[ 26 ] | 'inbound'  | response550 | [ hamilton ]
            otherDomains[ 0 ]  | 'outbound' | response250 | [ hamilton, otherDomains[ 0 ] ]
            otherDomains[ 1 ]  | 'outbound' | response250 | [ hamilton, otherDomains[ 1 ] ]
            otherDomains[ 2 ]  | 'outbound' | response250 | [ hamilton, otherDomains[ 2 ] ]
            otherDomains[ 3 ]  | 'outbound' | response250 | [ hamilton, otherDomains[ 3 ] ]
            otherDomains[ 4 ]  | 'outbound' | response250 | [ hamilton, otherDomains[ 4 ] ]
            otherDomains[ 5 ]  | 'outbound' | response250 | [ hamilton, otherDomains[ 5 ] ]
            otherDomains[ 6 ]  | 'outbound' | response250 | [ hamilton, otherDomains[ 6 ] ]
            otherDomains[ 7 ]  | 'outbound' | response250 | [ hamilton, otherDomains[ 7 ] ]
            otherDomains[ 8 ]  | 'outbound' | response250 | [ hamilton, otherDomains[ 8 ] ]
            otherDomains[ 9 ]  | 'outbound' | response250 | [ hamilton, otherDomains[ 9 ] ]
            otherDomains[ 10 ] | 'outbound' | response250 | [ hamilton, otherDomains[ 10 ] ]
            otherDomains[ 11 ] | 'outbound' | response250 | [ hamilton, otherDomains[ 11 ] ]
            otherDomains[ 12 ] | 'outbound' | response250 | [ hamilton, otherDomains[ 12 ] ]
            otherDomains[ 13 ] | 'outbound' | response250 | [ hamilton, otherDomains[ 13 ] ]
            otherDomains[ 14 ] | 'outbound' | response250 | [ hamilton, otherDomains[ 14 ] ]
            otherDomains[ 15 ] | 'outbound' | response250 | [ hamilton, otherDomains[ 15 ] ]
            otherDomains[ 16 ] | 'outbound' | response250 | [ hamilton, otherDomains[ 16 ] ]
            otherDomains[ 17 ] | 'outbound' | response250 | [ hamilton, otherDomains[ 17 ] ]
            otherDomains[ 18 ] | 'outbound' | response250 | [ hamilton, otherDomains[ 18 ] ]
            otherDomains[ 19 ] | 'outbound' | response250 | [ hamilton, otherDomains[ 19 ] ]
            otherDomains[ 20 ] | 'outbound' | response250 | [ hamilton, otherDomains[ 20 ] ]
            otherDomains[ 21 ] | 'outbound' | response250 | [ hamilton, otherDomains[ 21 ] ]
            otherDomains[ 22 ] | 'outbound' | response250 | [ hamilton, otherDomains[ 22 ] ]
            otherDomains[ 23 ] | 'outbound' | response250 | [ hamilton, otherDomains[ 23 ] ]
            otherDomains[ 24 ] | 'outbound' | response250 | [ hamilton, otherDomains[ 24 ] ]
            otherDomains[ 25 ] | 'outbound' | response250 | [ hamilton, otherDomains[ 25 ] ]
            otherDomains[ 26 ] | 'outbound' | response250 | [ hamilton, otherDomains[ 26 ] ]
            // 'user@domaincom'            | response550 
            // "user'name@domain.co.in"    | response550
	}
	
	@Unroll( "invalid address #inputAddress gives #value" )
	def "invalid address #inputAddress gives #value"() {
	    def resultMap
	    def resultString

        when:
            resultMap = rcptCommand.process( "RCPT TO:<${inputAddress}>", resultSetEM, [ forwardPath:  hamilton ] )
        then:
            println "command was EHLO, resultString is ${resultMap.resultString}"
            resultMap.resultString == value
            resultMap.bufferMap?.reversePath == resultAddress
            resultMap.prevCommandSet == resultSetEM
        where:
            inputAddress               | value                            | resultAddress
            'mkyong'                   | "501 Command not in proper form" | null 
            'mkyong@.com.my'           | "501 Command not in proper form" | null 
            'mkyong123@gmail.a'        | "501 Command not in proper form" | null 
            'mkyong123@.com'           | "501 Command not in proper form" | null 
            'mkyong123@.com.com'       | "501 Command not in proper form" | null 
            '.mkyong@mkyong.com'       | "501 Command not in proper form" | null 
            'mkyong()*@gmail.com'      | "501 Command not in proper form" | null 
            'mkyong@%*.com'            | "501 Command not in proper form" | null 
            'mkyong..2002@gmail.com'   | "501 Command not in proper form" | null 
            'mkyong.@gmail.com'        | "501 Command not in proper form" | null 
            'mkyong@mkyong@gmail.com'  | "501 Command not in proper form" | null 
            'mkyong@gmail.com.1a'      | "501 Command not in proper form" | null 
            '@yahoo.com'               | "501 Command not in proper form" | null 
            '.username@yahoo.com'      | "501 Command not in proper form" | null 
            'username@yahoo.com.'      | "501 Command not in proper form" | null 
            'username@yahoo..com'      | "501 Command not in proper form" | null 
            '.username@yahoo.com'      | "501 Command not in proper form" | null 
            'username@yahoo.com.'      | "501 Command not in proper form" | null 
            'username@yahoo..com'      | "501 Command not in proper form" | null 
            'username@yahoo.c'         | "501 Command not in proper form" | null 
            'username@yahoo.corporate' | "501 Command not in proper form" | null 
	}
	
	def "test happy path"() {
	    when:
	        def resultMap = rcptCommand.process( "RCPT TO:<${jackShell}>", resultSetEM, [ forwardPath:  [ hamilton ] ] )
	        def mailResponse = resultMap.resultString + crlf 
	        def bMap = resultMap.bufferMap
	    then:
	        mailResponse == "250 OK\r\n"
	        resultMap.prevCommandSet == resultSetEMR
	        bMap.forwardPath == [ hamilton, jackShell ]
	}

} // line 271

