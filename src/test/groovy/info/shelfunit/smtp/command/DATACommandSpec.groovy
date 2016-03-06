package info.shelfunit.smtp.command

import spock.lang.Specification

import org.junit.Rule
import org.junit.rules.TestName

import info.shelfunit.mail.meta.MetaProgrammer

class DATACommandSpec extends Specification {
    
    def crlf = "\r\n"
    static domainList = [ 'shelfunit.info', 'groovy-is-groovy.org' ]
    static dataCommand
    static hamilton = 'alexander@shelfunit.info'
    static gwShelf  = 'george.washington@shelfunit.info'
    static jAdamsShelf = 'john.adams@shelfunit.info'
    static jackShell   = 'oneill@shelfunit.info'
    static gwGroovy    = 'george.washington@groovy-is-groovy.org'
    static jaGroovy    = 'john.adams@groovy-is-groovy.org'
    static jackGroovy  = 'oneill@groovy-is-groovy.org'
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
        dataCommand = new DATACommand(  )
    }     // run before the first feature method
    
    def cleanupSpec() {
    }   // run after the last feature method
   
    def "test command with extra stuff"() {
        def bufferMapArg = [ forwardPath:[ 'alexander@shelfunit.info', 'george.washington@shelfunit.info' ], reversePath: 'oneill@stargate.mil' ]
        def prevCommandSetArg = [ 'EHLO', 'MAIL', 'RCPT' ] as Set
        when:
            def resultMap = dataCommand.process( 'DATA hello', prevCommandSetArg, bufferMapArg )
        then:
            resultMap.prevCommandSet == prevCommandSetArg
            resultMap.bufferMap == bufferMapArg
            resultMap.resultString == "501 Command not in proper form"
    }

	def "test handling wrong command"() {
	    def bufferMapArg = [ forwardPath:[ 'alexander@shelfunit.info', 'george.washington@shelfunit.info' ], reversePath: 'oneill@stargate.mil' ]
        def prevCommandSetArg = [ 'EHLO', 'MAIL', 'RCPT' ] as Set
        when:
            def resultMap = dataCommand.process( 'RCPT', prevCommandSetArg, bufferMapArg )
        then:
            resultMap.prevCommandSet == prevCommandSetArg
            resultMap.bufferMap == bufferMapArg
            resultMap.resultString == "503 Bad sequence of commands"
	}
	
	def "test commands in wrong order"() {
	    def bufferMapArg = [ forwardPath:[ 'alexander@shelfunit.info', 'george.washington@shelfunit.info' ], reversePath: 'oneill@stargate.mil' ]
        def prevCommandSetArg = [ 'EHLO', 'MAIL' ] as Set
        when:
            def resultMap = dataCommand.process( 'DATA', prevCommandSetArg, bufferMapArg )
        then:
            resultMap.prevCommandSet == prevCommandSetArg
            resultMap.bufferMap == bufferMapArg
            resultMap.resultString == "503 Bad sequence of commands"
	}

	def "test happy path"() {
	    
	    def bufferMapArg = [ forwardPath:[ 'alexander@shelfunit.info', 'george.washington@shelfunit.info' ], reversePath: 'oneill@stargate.mil' ]
        def prevCommandSetArg = [ 'EHLO', 'MAIL', 'RCPT' ] as Set
        when:
            def resultMap = dataCommand.process( 'DATA', prevCommandSetArg, bufferMapArg )
        then:
            resultMap.prevCommandSet == [ 'EHLO', 'MAIL', 'RCPT', 'DATA' ] as Set
            resultMap.bufferMap == bufferMapArg
            resultMap.resultString == "354 Start mail input; end with <CRLF>.<CRLF>"
	}

}

