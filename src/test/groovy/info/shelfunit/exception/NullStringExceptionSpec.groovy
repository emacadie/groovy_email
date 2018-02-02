package info.shelfunit.exception

import spock.lang.Specification

import org.junit.Rule
import org.junit.rules.TestName

class NullStringExceptionSpec extends Specification {
    @Rule 
    TestName name = new TestName()

    def setup() {
        println "\n--- Starting test ${name.methodName}"
    } // run before every feature method
    
    def cleanup() { } // run after every feature method
    
    def setupSpec() { } // run before the first feature method
    
    def cleanupSpec() { } // run after the last feature method

    def "try catching NullStringException"() {
        def someNum = 0
        def message
        when:
            try {
                if ( someNum == 0 ) {
                    throw new NullStringException()
                }
            } catch ( NullStringException nse ) {
                message = "In the catch for NullStringException"
                println nse.printReducedStackTrace( "info.shelfunit" )
            } catch ( Exception e ) {
                message =  "In the catch for generic Exception"
            }

        then:
            1 == 1
            println "Here is message: ${message}"
            message == "In the catch for NullStringException"

    } // def "try catching NullStringException"

    def "try catching generic Exception"() {
        def someNum = 0
        def message
        when:
            try {
                if ( someNum == 0 ) {
                    throw new Exception()
                }
            } catch ( NullStringException nse ) {
                message = "In the catch for NullStringException"
                println nse.printReducedStackTrace( "info.shelfunit" )
            } catch ( Exception e ) {
                message =  "In the catch for generic Exception"
            }

        then:
            1 == 1
            println "Here is message: ${message}"
            message == "In the catch for generic Exception"

    } // def "try using exception"() {
}

