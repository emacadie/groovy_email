package info.shelfunit.smtp.command

import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

import org.junit.Rule
import org.junit.rules.TestName

import info.shelfunit.mail.ConfigHolder
import info.shelfunit.mail.meta.MetaProgrammer

class MAILCommandSpec extends Specification {
    
    // if pre-defined, responses/inputs in unrolled tests must be defined here, not in method they are used
    static response250With8Bit = "250 <oneill@stargate.mil> Sender and 8BITMIME OK"
    static response503 = "503 Bad sequence of commands"
    static response501 = "501 Command not in proper form"
    static domainList = [ 'shelfunit2.info', 'groovy-is-groovy2.org' ]
    static mailCommand
    static sql
    static resultMap = [:]
    def crlf = "\r\n"

    @Rule 
    TestName name = new TestName()
    
    def setup() {
        println "\n--- Starting test ${name.methodName}"
        resultMap.clear()
    }          // run before every feature method
    def cleanup() {}        // run after every feature method
    def setupSpec() {
        MetaProgrammer.runMetaProgramming()
        MetaProgrammer.runMetaProgramming()
        ConfigHolder.instance.setConfObject( "src/test/resources/application.test.conf" )
        // sql = ConfigHolder.instance.getSqlObject()
        mailCommand = new MAILCommand( ConfigHolder.instance.getSqlObject(), domainList )
    }     // run before the first feature method
    
    def cleanupSpec() {}   // run after the last feature method

	def "test handling wrong command"() {
	    when:
	        resultMap = mailCommand.process( "MAIL FROM:<oneill@stargate.mil>", [ 'RCPT' ] as Set, [:] )
	        def mailResponse = resultMap.resultString + crlf 
	    then:
	        mailResponse == "503 Bad sequence of commands\r\n"
	        resultMap.prevCommandSet == [ "RCPT" ] as Set
	}
	
	@Unroll( "#command should result in #mailResponse" )
	def "#command results in #mailResponse"() {
	    expect:
	        mailResponse == mailCommand.process( "MAIL FROM:<oneill@stargate.mil>", [ command ] as Set, [:] ).resultString
	    
	    where:
            command | mailResponse
            'MAIL'  | response503
            'EXPN'  | response503
            'VRFY'  | response503
            'NOOP'  | response503
            'RCPT'  | response503
            'EHLO'  | "250 OK"
            'HELO'  | "250 OK"
            'RSET'  | "250 OK"
	}
	
	@Unroll( "#command gives #value with address #resultAddress" )
	def "#command gives #value with address #resultAddress"() {
	    def resultString

        when:
            resultMap = mailCommand.process( "MAIL FROM:<oneill@stargate.mil>", [ command ] as Set, [:] )
        then:
            println "command was ${command}, resultString is ${resultMap.resultString}"
            resultMap.resultString == value
            resultMap.bufferMap?.reversePath == resultAddress
        where:
            command | value         | resultAddress
            'EHLO'  | "250 OK"      | 'oneill@stargate.mil'
            'HELO'  | "250 OK"      | 'oneill@stargate.mil'
            'RSET'  | "250 OK"      | 'oneill@stargate.mil'
            'MAIL'  | response503   | null
            'EXPN'  | response503   | null
            'VRFY'  | response503   | null
            'NOOP'  | response503   | null
            'RCPT'  | response503   | null
	}
	
	@Unroll( "#command gives #value with address #resultAddress with BODY=8BITMIME at the end" )
	def "#command gives #value with address #resultAddress with BODY=8BITMIME at the end"() {
	    def resultString

        when:
            resultMap = mailCommand.process( "MAIL FROM:<oneill@stargate.mil> BODY=8BITMIME", [ command ] as Set, [:] )
        then:
            println "command was ${command}, resultString is ${resultMap.resultString}"
            resultMap.resultString == value
            resultMap.bufferMap?.reversePath == resultAddress
        where:
            command | value                 | resultAddress
            'EHLO'  | response250With8Bit   | 'oneill@stargate.mil'
            'HELO'  | response250With8Bit   | 'oneill@stargate.mil'
            'RSET'  | response250With8Bit   | 'oneill@stargate.mil'
            'MAIL'  | response503           | null
            'EXPN'  | response503           | null
            'VRFY'  | response503           | null
            'NOOP'  | response503           | null
            'RCPT'  | response503           | null
	}
	
	@Unroll( "#inputAddress gives #value with result Address the same" )
	def "#inputAddress gives #value with result Address the same"() {
	    def resultString

        when:
            resultMap = mailCommand.process( "MAIL FROM:<${inputAddress}>", [ 'EHLO' ] as Set, [:] )
        then:
            println "command was EHLO, resultString is ${resultMap.resultString}"
            resultMap.resultString == value
            resultMap.bufferMap?.reversePath == inputAddress
            resultMap.prevCommandSet == [ 'EHLO', 'MAIL' ] as Set
            resultMap.bufferMap?.messageDirection == direction
        where:
            inputAddress                | value     | direction
            'mkyong@yahoo.com'          | "250 OK"  | "inbound"
            'mkyong-100@yahoo.com'      | "250 OK"  | "inbound" 
            'mkyong.100@yahoo.com'      | "250 OK"  | "inbound" 
            'mkyong111@mkyong.com'      | "250 OK"  | "inbound"
            'mkyong-100@mkyong.net'     | "250 OK"  | "inbound"
            'mkyong.100@mkyong.com.au'  | "250 OK"  | "inbound"
            'mkyong@1.com'              | "250 OK"  | "inbound" 
            'mkyong@gmail.com.com'      | "250 OK"  | "inbound" 
            'mkyong+100@gmail.com'      | "250 OK"  | "inbound" 
            'mkyong-100@yahoo-test.com' | "250 OK"  | "inbound" 
            'howTuser@domain.com'       | "250 OK"  | "inbound" 
            'user@domain.co.in'         | "250 OK"  | "inbound" 
            'user1@domain.com'          | "250 OK"  | "inbound" 
            'user.name@domain.com'      | "250 OK"  | "inbound" 
            'user_name@domain.co.in'    | "250 OK"  | "inbound" 
            'user-name@domain.co.in'    | "250 OK"  | "inbound" 
            // 'user@domaincom'            | "250 OK" 
            'user@domain.com'           | "250 OK"  | "inbound" 
            'user@domain.co.in'         | "250 OK"  | "inbound" 
            'user.name@domain.com'      | "250 OK" | "inbound" 
            // "user'name@domain.co.in"    | "250 OK"
            'user@domain.com'           | "250 OK"  | "inbound" 
            'user@domain.co.in'         | "250 OK"  | "inbound" 
            'user.name@domain.com'      | "250 OK"  | "inbound" 
            'user_name@domain.com'      | "250 OK"  | "inbound" 
            'username@yahoo.corporate.in'  | "250 OK"  | "inbound" 
            'mkyong@shelfunit2.info'       | "250 OK"  | "outbound"
            'mkyong-100@shelfunit.info'    | "250 OK"  | "inbound" 
            'mkyong@groovy-is-groovy2.org' | "250 OK"  | "outbound" 
            'mkyong@groovy-is-groovy3.org' | "250 OK"  | "inbound"
	}
	
	@Unroll( "invalid address #inputAddress gives #value" )
	def "invalid address #inputAddress gives #value"() {
	    def resultString

        when:
            resultMap = mailCommand.process( "MAIL FROM:<${inputAddress}>", [ 'EHLO' ] as Set, [:] )
        then:
            println "command was EHLO, resultString is ${resultMap.resultString}"
            resultMap.resultString == value
            resultMap.bufferMap?.reversePath == resultAddress
            resultMap.prevCommandSet == [ 'EHLO' ] as Set
        where:
            inputAddress                | value         | resultAddress
            'mkyong'                    | response501   | null 
            'mkyong@.com.my'            | response501   | null 
            'mkyong123@gmail.a'         | response501   | null 
            'mkyong123@.com'            | response501   | null 
            'mkyong123@.com.com'        | response501   | null 
            '.mkyong@mkyong.com'        | response501   | null 
            'mkyong()*@gmail.com'       | response501   | null 
            'mkyong@%*.com'             | response501   | null 
            'mkyong..2002@gmail.com'    | response501   | null 
            'mkyong.@gmail.com'         | response501   | null 
            'mkyong@mkyong@gmail.com'   | response501   | null 
            'mkyong@gmail.com.1a'       | response501   | null 
            '@yahoo.com'                | response501   | null 
            '.username@yahoo.com'       | response501   | null 
            'username@yahoo.com.'       | response501   | null 
            'username@yahoo..com'       | response501   | null 
            '.username@yahoo.com'       | response501   | null 
            'username@yahoo.com.'       | response501   | null 
            'username@yahoo..com'       | response501   | null 
            'username@yahoo.c'          | response501   | null 
            'username@yahoo.corporate'  | response501   | null 
	}
	
	def "test happy path"() {
	    when:
	        resultMap = mailCommand.process( "MAIL FROM:<oneill@stargate.mil>", ['EHLO'] as Set, [:] )
	        def mailResponse = resultMap.resultString + crlf 
	        def bMap = resultMap.bufferMap
	    then:
	        mailResponse == "250 OK\r\n"
	        resultMap.prevCommandSet == [ "EHLO", "MAIL" ] as Set
	        bMap.reversePath == 'oneill@stargate.mil'
	        resultMap.bufferMap.messageDirection == 'inbound'
	}
	
	@Ignore
	def "always ignore"() {
	}
	
	@Ignore
	def "ad-hoc test"() {
	    when:
	        resultMap = mailCommand.process( "", ['HELO'] as Set, [:] )
	        def mailResponse = resultMap.resultString + crlf 
	        def bMap = resultMap.bufferMap
	    then:
	        mailResponse == "250 OK\r\n"
	        resultMap.prevCommandSet == [ "EHLO", "MAIL" ] as Set
	        bMap.reversePath == 'oneill@stargate.mil'
	        resultMap.bufferMap.messageDirection == 'inbound'
	}

}

