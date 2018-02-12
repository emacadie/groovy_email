package info.shelfunit.mail.meta

import info.shelfunit.mail.ConfigHolder

import spock.lang.Specification

import org.junit.Rule
import org.junit.rules.TestName

class StringMetaProgrammerSpec extends Specification {

    @Rule 
    TestName name = new TestName()
    
    def crlf = "\r\n"

    def setup() {
        println "\n--- Starting test ${name.methodName}"
    }          // run before every feature method
    
    def cleanup() {}        // run after every feature method
    
    def setupSpec() {
        MetaProgrammer.runMetaProgramming()
        ConfigHolder.instance.setConfObject( "src/test/resources/application.test.conf" )
    }     // run before the first feature method
    
    def cleanupSpec() { } // run after the last feature method

    def "test checkForCRLF"() {
        when:
            def testString = "hello"
        then:
            testString.endsWith( crlf ) == false
        when:
            testString       = testString.checkForCRLF()
            def secondString = testString.checkForCRLF()
        then:
            testString.endsWith( crlf )   == true
            secondString.endsWith( crlf ) == true
    }

    def "test that we do not add CRLF to a string that already has it"() {
        when:
            def testString = "hello\r\n"
        then:
            testString.endsWith( crlf ) == true
        when:
            def secondString = testString.checkForCRLF()
        then:
            secondString.endsWith( "\r\n" )     == true
            secondString.endsWith( "\r\n\r\n" ) == false
    }

    def "test getDomain"() {
        when:
            def testString = 'EHLO this.is.a.domain'
        then:
            testString.getDomain() != ' this.is.a.domain'
            testString.getDomain() == 'this.is.a.domain'
    }

    def "test safeSubString"() {
        when:
            def testString = "this is a string"
        then:
            testString.substring( 0, 5 )         == "this "
            testString.safeSubstring( 0, 5 )     == "this "
            testString.safeSubstring( -5, 5 )    == "this "
            testString.safeSubstring( 0, 100 )   == "this is a string"
            testString.safeSubstring( 100, 200 ) == "this is a string"
        

    }

} // end class StringMetaProgrammerSpec

