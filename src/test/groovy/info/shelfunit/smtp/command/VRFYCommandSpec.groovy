package info.shelfunit.smtp.command

import spock.lang.Specification
// import spock.lang.Unroll

import org.junit.Rule
import org.junit.rules.TestName

import info.shelfunit.mail.meta.MetaProgrammer

// import groovy.sql.Sql

class VRFYCommandSpec extends Specification {
    
    // def crlf = "\r\n"
    // static domainList = [ 'shelfunit.info', 'groovy-is-groovy.org' ]
    // static vrfyCommand

    @Rule 
    TestName name = new TestName()
    
    def setup() {
        println "\n--- Starting test ${name.methodName}"
    }          // run before every feature method
    def cleanup() {}        // run after every feature method
    
    def setupSpec() {
        // MetaProgrammer.runMetaProgramming()
        // vrfyCommand = new VRFYCommand( )
    }     // run before the first feature method
    
    def cleanupSpec() {
    }

}

