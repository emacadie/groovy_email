package info.shelfunit.socket

import spock.lang.Specification
import java.io.InputStream
import java.io.OutputStream
import java.io.BufferedReader

// import com.google.common.io.ByteStreams

import org.junit.Rule
import org.junit.rules.TestName

class BinarySMTPSocketWorkerSpec extends Specification {
    
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
    def crlf = "\r\n"
    
    @Rule 
    TestName name = new TestName()

	def "test handling EHLO"() {
	    println "\n\n--- Starting test ${name.methodName}"
	    def serverName = "www.groovymail.org"
	    def ssWorker = new BinarySMTPSocketWorker( Mock( InputStream ), Mock( OutputStream ), serverName )
	    
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
	    println "\n\n---  Starting test ${name.methodName}"
	    def serverName = "www.groovymail.org"
	    def ssWorker = new BinarySMTPSocketWorker( Mock( InputStream ), Mock( OutputStream ), serverName )
	    
	    expect:
	        ssWorker.serverName == "www.groovymail.org"
	    
	    def domain = "hot-groovy.com"
	    when:
	        def ehloResponse = ssWorker.handleMessage( "HELO ${domain}" )
	    then:
	        ehloResponse == "250 Hello ${domain}\r\n"
	}
	
	def "test streams"() {
	    println "\n\n--- Starting test ${name.methodName}"
	    when:
            def serverName = "www.groovymail.org"
            
            def domain = "hot-groovy.com"
            byte[] data = "EHLO ${domain}${crlf}DATA${crlf}JJJ${crlf}.${crlf}QUIT${crlf}".getBytes()
    
            InputStream input   = new ByteArrayInputStream( data );
            OutputStream output = new ByteArrayOutputStream() 
            
            def ssWorker = new BinarySMTPSocketWorker( input, output, serverName )
            ssWorker.doWork()
            def ehloResponse = ssWorker.handleMessage( "HELO ${domain}" )
        then:

            println "output to string: +++++\n${output.toString()}"
            println "+++++ end of output"
            ehloResponse == "250 Hello ${domain}\r\n"
        when:
            ehloResponse = ssWorker.handleMessage( "EHLO ${domain}" )
        then:
            ehloResponse == "250-Hello ${domain}\n" +
	        "250 HELP\r\n"
	}
	
	def "test streams again"() {
	    println "\n\n--- Starting test ${name.methodName}"
	    /*
	    when:
            def serverName = "www.groovymail.org"
            def crlf = "\r\n"
            def mIs = Mock( InputStream )
            def mOs = Mock( OutputStream )
            def domain = "hot-groovy.com"
            byte[] data = "EHLO ${domain}${crlf}DATA${crlf}JJJ${crlf}.${crlf}QUIT${crlf}".getBytes();
    
            InputStream input = new ByteArrayInputStream( data );
            OutputStream output = new ByteArrayOutputStream() 
            InputStream first = new ByteArrayInputStream( new byte[ 1024 ] )
            
            def ssWorker = new BinarySMTPSocketWorker( mIs, mOs, serverName )
            ssWorker.doWork()
            byte[] dataA = "DATA${crlf}JJJ${crlf}.${crlf}QUIT${crlf}".getBytes();
            
            // InputStream inputA = new ByteArrayInputStream( dataA );
            InputStream inputA = new ByteArrayInputStream( dataA );
            
            
	            def ehloResponse = ssWorker.handleMessage( "HELO ${domain}" )
	        then:
	            19 * mIs.read()
	            def exA = thrown( Exception )
	            println "exA.message: ${exA.message}"
	            exA.printStackTrace()
	            println "output to string: ${output.toString()}"
	            // def copy = ByteStreams.copy( first, output )
	            // println "Here is copy: ${copy}"
	            ehloResponse == "250 Hello ${domain}\r\n"
	            */

	}
}

