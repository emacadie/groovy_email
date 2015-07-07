package info.shelfunit.socket

import spock.lang.Specification
import spock.lang.Unroll

import org.junit.Rule
import org.junit.rules.TestName

import info.shelfunit.mail.MailRunner
import info.shelfunit.socket.command.MAILCommand

// import org.xbill.DNS.Address

// import groovy.mock.interceptor.StubFor

class MAILCommandSpec extends Specification {
    
    def crlf = "\r\n"

    @Rule 
    TestName name = new TestName()
    
    def setup() {
        println "\n--- Starting test ${name.methodName}"
    }          // run before every feature method
    def cleanup() {}        // run after every feature method
    def setupSpec() {
        MailRunner.runMetaProgramming()
    }     // run before the first feature method
    
    def cleanupSpec() {}   // run after the last feature method

	def "test handling wrong command"() {
	    def mailCommand = new MAILCommand()
	    
	    when:
	        def resultMap = mailCommand.process( "MAIL FROM:<oneill@stargate.mil>", ['RCPT'], [:] )
	        def mailResponse = resultMap.resultString + crlf 
	    then:
	        mailResponse == "503 Bad sequence of commands\r\n"
	        resultMap.prevCommandList == [ "RCPT" ]
	}
	
	@Unroll( "#command should result in #mailResponse" )
	def "#command results in #mailResponse"() {
	    def resultMap
	    def mailCommand = new MAILCommand()
	    expect:
	        mailResponse == mailCommand.process( "MAIL FROM:<oneill@stargate.mil>", [ command ], [:] ).resultString
	    
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
                resultMap = mailCommand.process( "MAIL FROM:<oneill@stargate.mil>", [ command ], [:] )
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
                resultMap = mailCommand.process( "MAIL FROM:<${inputAddress}>", [ 'EHLO' ], [:] )
            then:
                println "command was EHLO, resultString is ${resultMap.resultString}"
                resultMap.resultString == value
                resultMap.bufferMap?.reversePath == inputAddress
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
	}
	
	def "test happy path"() {
	    def mailCommand = new MAILCommand()
	    when:
	        def resultMap = mailCommand.process( "MAIL FROM:<oneill@stargate.mil>", ['EHLO'], [:] )
	        def mailResponse = resultMap.resultString + crlf 
	        def bMap = resultMap.bufferMap
	    then:
	        mailResponse == "250 OK\r\n"
	        resultMap.prevCommandList == [ "EHLO", "MAIL" ]
	        bMap.reversePath == 'oneill@stargate.mil'
	}

}

