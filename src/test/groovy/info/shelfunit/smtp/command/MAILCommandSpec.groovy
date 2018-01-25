package info.shelfunit.smtp.command

import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Stepwise
import spock.lang.Unroll

import org.junit.Rule
import org.junit.rules.TestName

import info.shelfunit.mail.ConfigHolder
import info.shelfunit.mail.meta.MetaProgrammer

@Stepwise
class MAILCommandSpec extends Specification {
    
    // if pre-defined, responses/inputs in unrolled tests must be defined here, not in method they are used
    static response250With8Bit = "250 <oneill@stargate.mil> Sender and 8BITMIME OK"
    static response250OK = "250 OK"
    static response503   = "503 Bad sequence of commands"
    static response501   = "501 Command not in proper form"
    static domainList    = [ 'shelfunit2.info', 'groovy-is-groovy2.org' ]
    static resultMap     = [:]
    def crlf             = "\r\n"
    static mailCommand

    @Rule 
    TestName name = new TestName()
    
    def setup() {
        println "\n--- Starting test ${name.methodName}"
        resultMap.clear()
    }          // run before every feature method
    def cleanup() {}        // run after every feature method
    def setupSpec() {
        MetaProgrammer.runMetaProgramming()
        ConfigHolder.instance.setConfObject( "src/test/resources/application.test.conf" )
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
            'EHLO'  | response250OK
            'HELO'  | response250OK
            'RSET'  | response250OK
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
            'EHLO'  | response250OK | 'oneill@stargate.mil'
            'HELO'  | response250OK | 'oneill@stargate.mil'
            'RSET'  | response250OK | 'oneill@stargate.mil'
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

// MAIL FROM:<shelfunit@outlook.com> AUTH=<>
    @Unroll( "#command gives #value with address #resultAddress with AUTH=<> BODY=7BIT at the end" )
    def "#command gives #value with address #resultAddress with AUTH=<> BODY=7BIT at the end"() {
	    def resultString

        when:
            resultMap = mailCommand.process( "MAIL FROM:<oneill@stargate.mil> AUTH=<> BODY=7BIT", [ command ] as Set, [:] )
        then:
            println "command was ${command}, resultString is ${resultMap.resultString}"
            resultMap.resultString == value
            resultMap.bufferMap?.reversePath == resultAddress
        where:
            command | value         | resultAddress
            'EHLO'  | response250OK | 'oneill@stargate.mil'
            'HELO'  | response250OK | 'oneill@stargate.mil'
            'RSET'  | response250OK | 'oneill@stargate.mil'
            'MAIL'  | response503   | null
            'EXPN'  | response503   | null
            'VRFY'  | response503   | null
            'NOOP'  | response503   | null
            'RCPT'  | response503   | null
	}

// MAIL FROM:<shelfunit@outlook.com> AUTH=<>
    @Unroll( "#command gives #value with address #resultAddress with AUTH=<> at the end" )
    def "#command gives #value with address #resultAddress with AUTH=<>  at the end"() {
	    def resultString

        when:
            resultMap = mailCommand.process( "MAIL FROM:<oneill@stargate.mil> AUTH=<>", [ command ] as Set, [:] )
        then:
            println "command was ${command}, resultString is ${resultMap.resultString}"
            resultMap.resultString == value
            resultMap.bufferMap?.reversePath == resultAddress
        where:
            command | value         | resultAddress
            'EHLO'  | response250OK | 'oneill@stargate.mil'
            'HELO'  | response250OK | 'oneill@stargate.mil'
            'RSET'  | response250OK | 'oneill@stargate.mil'
            'MAIL'  | response503   | null
            'EXPN'  | response503   | null
            'VRFY'  | response503   | null
            'NOOP'  | response503   | null
            'RCPT'  | response503   | null
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
            inputAddress                | value         | direction
            'mkyong@yahoo.com'          | response250OK | "inbound"
            'mkyong-100@yahoo.com'      | response250OK | "inbound" 
            'mkyong.100@yahoo.com'      | response250OK | "inbound" 
            'mkyong111@mkyong.com'      | response250OK | "inbound"
            'mkyong-100@mkyong.net'     | response250OK | "inbound"
            'mkyong.100@mkyong.com.au'  | response250OK | "inbound"
            'mkyong@1.com'              | response250OK | "inbound" 
            'mkyong@gmail.com.com'      | response250OK | "inbound" 
            'mkyong+100@gmail.com'      | response250OK | "inbound" 
            'mkyong-100@yahoo-test.com' | response250OK | "inbound" 
            'howTuser@domain.com'       | response250OK | "inbound" 
            'user@domain.co.in'         | response250OK | "inbound" 
            'user1@domain.com'          | response250OK | "inbound" 
            'user.name@domain.com'      | response250OK | "inbound" 
            'user_name@domain.co.in'    | response250OK | "inbound" 
            'user-name@domain.co.in'    | response250OK | "inbound" 
            // 'user@domaincom'            | response250OK 
            'user@domain.com'           | response250OK | "inbound" 
            'user@domain.co.in'         | response250OK | "inbound" 
            'user.name@domain.com'      | response250OK | "inbound" 
            // "user'name@domain.co.in"    | response250OK
            'user@domain.com'           | response250OK | "inbound" 
            'user@domain.co.in'         | response250OK | "inbound" 
            'user.name@domain.com'      | response250OK | "inbound" 
            'user_name@domain.com'      | response250OK | "inbound" 
            'username@yahoo.corporate.in'  | response250OK | "inbound" 
            'mkyong@shelfunit2.info'       | response250OK | "outbound"
            'mkyong-100@shelfunit.info'    | response250OK | "inbound" 
            'mkyong@groovy-is-groovy2.org' | response250OK | "outbound" 
            'mkyong@groovy-is-groovy3.org' | response250OK | "inbound"
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

