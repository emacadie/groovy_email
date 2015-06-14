package info.shelfunit.socket

import spock.lang.Specification
import spock.lang.Ignore
import java.io.InputStream
import java.io.OutputStream
import java.io.BufferedReader

import com.google.common.io.ByteStreams

import org.junit.Rule
import org.junit.rules.TestName

class SMTPSocketWorkerSpec extends Specification {
    
    def setup() {}          // run before every feature method
    def cleanup() {}        // run after every feature method
    def setupSpec() {    
                StringBuffer.metaClass.endsWith = { end ->
            if ( delegate.length() < end.length() ) {
                return false
            } else if ( delegate.substring( ( delegate.length() - end.length() ), delegate.length() ).equals( end ) ) {
                return true
            } else {
                return false
            }   
        }
        StringBuffer.metaClass.clear = { ->
            delegate.delete( 0, delegate.length() )
        }
    }     // run before the first feature method
    def cleanupSpec() {}   // run after the last feature method
    
    @Rule 
    TestName name = new TestName()

	def "test handling EHLO"() {
	    println "--- Starting test ${name.methodName}"
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
	    println "--- XX Starting test ${name.methodName}"
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
	
	@Ignore
	def "test streams"() {
	    println "--- Starting test ${name.methodName}"
	    when:
            def serverName = "www.groovymail.org"
            def crlf = "\r\n"
            def mIs = Mock( InputStream )
            def mOs = Mock( OutputStream )
            def domain = "hot-groovy.com"
            byte[] data = "EHLO ${domain}${crlf}DATA${crlf}JJJ${crlf}.${crlf}QUIT${crlf}".getBytes()
    
            InputStream input = new ByteArrayInputStream( data );
            OutputStream output = new ByteArrayOutputStream() 
            
            def ssWorker = new SMTPSocketWorker( input, output, serverName )
            ssWorker.doWork()
            input = new ByteArrayInputStream( "DATA${crlf}JJJ${crlf}.${crlf}QUIT${crlf}".getBytes() )
            def ehloResponse = ssWorker.handleMessage( "HELO ${domain}" )
	    then:
            // def exA = thrown( Exception )
            // println "exA.message: ${exA.message}"
            // exA.printStackTrace()
            println "output to string: ++++\n${output.toString()}"
            println "++++ end of output"
            // def copy = ByteStreams.copy( first, output )
            // println "Here is copy: ${copy}"
            ehloResponse == "250 Hello ${domain}\r\n"
	    
	}
}

