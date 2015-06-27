package info.shelfunit.prep


import spock.lang.Specification
import spock.lang.Ignore
import java.io.InputStream
import java.io.OutputStream
import java.io.BufferedReader

import org.apache.commons.io.IOUtils

import org.junit.Rule
import org.junit.rules.TestName

import info.shelfunit.mail.MailRunner
import info.shelfunit.socket.command.EHLOCommand

import org.xbill.DNS.Address

import groovy.mock.interceptor.StubFor

class FirstPrepSpec extends Specification {
    
    @Rule 
    TestName name = new TestName()
    
    def setup() {
        println "\n--- Starting test ${name.methodName}"
    }          // run before every feature method
    def cleanup() {}        // run after every feature method
    def setupSpec() {
        println "In ${this.class.name}"
        println System.properties[ 'database.prep' ] 
        if ( System.getProperty( "database.prep" ) == 'true' ) {
            println "database.prep is true"
        } else {
            println "database.prep not set"
        }
        

    }     // run before the first feature method
    
    def cleanupSpec() {}   // run after the last feature method

    def "first test"() {
        expect:
            1 == 1
    }
    
}


