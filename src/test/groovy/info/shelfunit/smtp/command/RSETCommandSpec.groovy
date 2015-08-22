package info.shelfunit.smtp.command

import spock.lang.Specification
import spock.lang.Unroll

// import spock.lang.Ignore

import org.junit.Rule
import org.junit.rules.TestName

import info.shelfunit.mail.MetaProgrammer

class RSETCommandSpec extends Specification {
    
    def crlf = "\r\n"
    static domainList = [ 'shelfunit.info', 'groovy-is-groovy.org' ]
    static sql
    static rsetCommand

    @Rule 
    TestName name = new TestName()
    
    def setup() {
        println "\n--- Starting test ${name.methodName}"
    }          // run before every feature method
    def cleanup() {}        // run after every feature method
    
    def setupSpec() {
        MetaProgrammer.runMetaProgramming()
                
        rsetCommand = new RSETCommand( )
    }     // run before the first feature method
    
    def cleanupSpec() {

    }   // run after the last feature method

	def "test handling wrong command"() {
	    when:
	        def resultMap = rsetCommand.process( "MAIL FROM:<oneill@stargate.mil>", [ 'RCPT' ] as Set, [:] )
	        def mailResponse = resultMap.resultString + crlf 
	    then:
	        mailResponse == "250 OK\r\n"
	        resultMap.prevCommandSet == [ ] as Set
	}

	@Unroll( "#inputAddress gives #value" )
	def "#inputAddress gives #value"() {
	    def resultMap
	    def resultString

        when:
            resultMap = rsetCommand.process( "RSET", [ 'EHLO', 'MAIL' ] as Set, [:] )
        then:
            resultMap.resultString == value
            resultMap.prevCommandSet == [  ] as Set
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
            resultMap = rsetCommand.process( "RSET", [ 'EHLO', 'MAIL', 'RCPT' ] as Set, [:] )
        then:
            resultMap.resultString == value
            resultMap.prevCommandSet == [  ] as Set
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
            resultMap = rsetCommand.process( "RSET", prevCommandSet, [:] )
        then:
            println "command was EHLO, resultString is ${resultMap.resultString}"
            resultMap.resultString == value
            resultMap.prevCommandSet == [  ] as Set
        where:
            inputAddress                | prevCommandSet | value    
            'george.washington@shelfunit.info'  | [ 'EHLO', 'MAIL', 'RCPT' ] as Set | "250 OK"
            'john.adams@shelfunit.info' | [ 'EHLO', 'MAIL', 'RCPT' ] as Set | "250 OK"
            'oneill@shelfunit.info'     | [ 'EHLO', 'MAIL', 'RCPT' ] as Set | "250 OK"
            'george.washington@groovy-is-groovy.org'  | [ 'EHLO', 'MAIL', 'RCPT' ] as Set | "250 OK"
            'john.adams@groovy-is-groovy.org' | [ 'EHLO', 'MAIL', 'RCPT' ] as Set | "250 OK"
            'oneill@groovy-is-groovy.org'     | [ 'EHLO', 'MAIL', 'RCPT' ] as Set | "250 OK"
            'george.washington@shelfunit.info'  | [ 'EHLO', 'MAIL' ] as Set | "250 OK"
            'john.adams@shelfunit.info' | [ 'EHLO', 'MAIL' ] as Set | "250 OK"
            'oneill@shelfunit.info'     | [ 'EHLO', 'MAIL' ] as Set | "250 OK"
            'george.washington@groovy-is-groovy.org'  | [ 'EHLO', 'MAIL' ] as Set | "250 OK"
            'john.adams@groovy-is-groovy.org' | [ 'EHLO', 'MAIL' ] as Set | "250 OK"
            'oneill@groovy-is-groovy.org'     | [ 'EHLO', 'MAIL' ] as Set | "250 OK"
	}
	
	@Unroll( "#inputAddress with wrong prev command sequence gives #value" )
	def "#inputAddress with wrong prev command sequence gives #value"() {
	    def resultMap
	    def resultString

        when:
            resultMap = rsetCommand.process( "RSET", prevCommandSet, [:] )
        then:
            println "command was EHLO, resultString is ${resultMap.resultString}"
            resultMap.resultString == value
            resultMap.prevCommandSet.isEmpty()
        where:
            inputAddress                        | prevCommandSet | value    
            'george.washington@shelfunit.info'  | [ 'EHLO', 'MAIL' ] as Set | "250 OK"
            'john.adams@shelfunit.info'         | [ 'EHLO', 'MAIL' ] as Set | "250 OK"
	}

	@Unroll( "#inputAddress with wrong domain gives #value" )
	def "#inputAddress with wrong domain gives #value"() {
	    def resultMap
	    def resultString

        when:
            resultMap = rsetCommand.process( "RSET", [ 'EHLO', 'MAIL' ] as Set, [:] )
        then:
            println "command was EHLO, resultString is ${resultMap.resultString}"
            resultMap.resultString == value
            resultMap.prevCommandSet == [  ] as Set
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
            'george.washington@mtvernon.co' | "250 OK"
            'john.adams@his-rotundity.org'  | "250 OK"
            'oneill@stargate.mil'       | "250 OK"
	}

	def "test happy path"() {
	    when:
	        def resultMap = rsetCommand.process( "RSET", [ 'EHLO', 'MAIL' ] as Set, [:] )
	        def mailResponse = resultMap.resultString + crlf 
	        def bMap = resultMap.bufferMap
	    then:
	        mailResponse == "250 OK\r\n"
	        resultMap.prevCommandSet == [ ] as Set
	        bMap.forwardPath == null
	}

}

