package info.shelfunit.socket

import spock.lang.Specification
import java.io.InputStream
import java.io.OutputStream
import java.io.BufferedReader

class SMTPSocketWorkerSpec extends Specification {
    
    def setup() {}          // run before every feature method
    def cleanup() {}        // run after every feature method
    def setupSpec() {    }     // run before the first feature method
    def cleanupSpec() {}   // run after the last feature method

	def "test handling EHLO"() {
	    def serverName = "www.groovymail.org"
	    def ssWorker = new SMTPSocketWorker( Mock( InputStream ), Mock( OutputStream ), serverName )
	    
	    expect:
	        ssWorker.serverName == "www.groovymail.org"
	    
	    def domain = "hot-groovy.com"
	    when:
	        def ehloResponse = ssWorker.handleMessage( "EHLO ${domain}" )
	    then:
	        ehloResponse == "250-Hello ${domain}\n" +
	        "250 HELP\r\n"
	}
	
	def "test handling HELO"() {
	    def serverName = "www.groovymail.org"
	    def ssWorker = new SMTPSocketWorker( Mock( InputStream ), Mock( OutputStream ), serverName )
	    
	    expect:
	        ssWorker.serverName == "www.groovymail.org"
	    
	    def domain = "hot-groovy.com"
	    when:
	        def ehloResponse = ssWorker.handleMessage( "HELO ${domain}" )
	    then:
	        ehloResponse == "250 Hello ${domain}\r\n"
	}
}

