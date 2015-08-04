package info.shelfunit.socket.command

import spock.lang.Specification
import spock.lang.Unroll

import org.junit.Rule
import org.junit.rules.TestName

import info.shelfunit.mail.MetaProgrammer

class MAILCommandSpec extends Specification {
    
    def crlf = "\r\n"

    @Rule 
    TestName name = new TestName()
    
    def setup() {
        println "\n--- Starting test ${name.methodName}"
    }          // run before every feature method
    def cleanup() {}        // run after every feature method
    def setupSpec() {
        MetaProgrammer.runMetaProgramming()
    }     // run before the first feature method
    
    def cleanupSpec() {}   // run after the last feature method

	def "test handling wrong command"() {
	    def mailCommand = new MAILCommand()
	    
	    when:
	        def resultMap = mailCommand.process( "MAIL FROM:<oneill@stargate.mil>", [ 'RCPT' ] as Set, [:] )
	        def mailResponse = resultMap.resultString + crlf 
	    then:
	        mailResponse == "503 Bad sequence of commands\r\n"
	        resultMap.prevCommandSet == [ "RCPT" ] as Set
	}
	
	@Unroll( "#command should result in #mailResponse" )
	def "#command results in #mailResponse"() {
	    def resultMap
	    def mailCommand = new MAILCommand()
	    expect:
	        mailResponse == mailCommand.process( "MAIL FROM:<oneill@stargate.mil>", [ command ] as Set, [:] ).resultString
	    
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
	
	@Unroll( "#command gives #value with address #resultAddress" )
	def "#command gives #value with address #resultAddress"() {
	    def resultMap
	    def mailCommand = new MAILCommand()
	    def resultString

        when:
            resultMap = mailCommand.process( "MAIL FROM:<oneill@stargate.mil>", [ command ] as Set, [:] )
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
	
	@Unroll( "#inputAddress gives #value with result Address the same" )
	def "#inputAddress gives #value with result Address the same"() {
	    def resultMap
	    def mailCommand = new MAILCommand()
	    def resultString

        when:
            resultMap = mailCommand.process( "MAIL FROM:<${inputAddress}>", [ 'EHLO' ] as Set, [:] )
        then:
            println "command was EHLO, resultString is ${resultMap.resultString}"
            resultMap.resultString == value
            resultMap.bufferMap?.reversePath == inputAddress
            resultMap.prevCommandSet == [ 'EHLO', 'MAIL' ] as Set
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
	
	@Unroll( "invalid address #inputAddress gives #value" )
	def "invalid address #inputAddress gives #value"() {
	    def resultMap
	    def mailCommand = new MAILCommand()
	    def resultString

        when:
            resultMap = mailCommand.process( "MAIL FROM:<${inputAddress}>", [ 'EHLO' ] as Set, [:] )
        then:
            println "command was EHLO, resultString is ${resultMap.resultString}"
            resultMap.resultString == value
            resultMap.bufferMap?.reversePath == resultAddress
            resultMap.prevCommandSet == [ 'EHLO' ] as Set
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
	    def mailCommand = new MAILCommand()
	    when:
	        def resultMap = mailCommand.process( "MAIL FROM:<oneill@stargate.mil>", ['EHLO'] as Set, [:] )
	        def mailResponse = resultMap.resultString + crlf 
	        def bMap = resultMap.bufferMap
	    then:
	        mailResponse == "250 OK\r\n"
	        resultMap.prevCommandSet == [ "EHLO", "MAIL" ] as Set
	        bMap.reversePath == 'oneill@stargate.mil'
	}

}

