package info.shelfunit.socket

import spock.lang.Specification
import spock.lang.Ignore
import java.io.InputStream
import java.io.OutputStream
import java.io.BufferedReader

import org.apache.commons.io.IOUtils
//  import com.google.common.io.ByteStreams

import org.junit.Rule
import org.junit.rules.TestName


class ModularSMTPSocketWorkerSpec extends Specification {
    
    def crlf = "\r\n"
    
    def setup() {}          // run before every feature method
    def cleanup() {}        // run after every feature method
    def setupSpec() {
        
        // ExpandoMetaClass.enableGlobally()
        java.util.List.metaClass.lastItem = {
            if ( delegate.size() != 0 ) { 
                delegate.last()
            }
        }
        
        StringBuffer.metaClass.endsWith = { end ->
            if ( delegate.length() < end.length() ) {
                return false
            } else if ( delegate.substring( ( delegate.length() - end.length() ), delegate.length() ).equals( end ) ) {
                return true
            } else {
                return false
            }   
        }
        StringBuffer.metaClass.startsWith = { strt ->
            if ( delegate.length() < strt.length() ) {
                return false
            } else if ( delegate.substring( 0, strt.length() ).equals( strt ) ) {
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
	    println "\n--- Starting test ${name.methodName}"
	    def serverName = "www.groovymail.org"
	    def ssWorker = new ModularSMTPSocketWorker( Mock( InputStream ), Mock( OutputStream ), serverName )
	    
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
	    println "\n--- XX Starting test ${name.methodName}"
	    def serverName = "www.groovymail.org"
	    def ssWorker = new ModularSMTPSocketWorker( Mock( InputStream ), Mock( OutputStream ), serverName )
	    
	    expect:
	        ssWorker.serverName == "www.groovymail.org"
	    
	    def domain = "hot-groovy.com"
	    when:
	        def ehloResponse = ssWorker.handleMessage( "HELO ${domain}" )
	    then:
	        ehloResponse == "250 Hello ${domain}\r\n"
	}
	
	def "test handling old commands"() {
	    println "\n--- Starting test ${name.methodName}"
	    def serverName = "www.groovymail.org"
	    def ssWorker = new ModularSMTPSocketWorker( Mock( InputStream ), Mock( OutputStream ), serverName )
	    
	    expect:
	        ssWorker.serverName == "www.groovymail.org"
	    
	    def domain = "hot-groovy.com"
	    when: "Sending SAML"
	        def ehloResponse = ssWorker.handleMessage( "SAML ${domain}" )
	    then:
	        ehloResponse == "502 Command not implemented\r\n"
	    when: "Sending SEND"
	        ehloResponse = ssWorker.handleMessage( "SEND ${domain}" )
	    then:
	        ehloResponse == "502 Command not implemented\r\n"
	    when: "Sending SOML"
	        ehloResponse = ssWorker.handleMessage( "SOML ${domain}" )
	    then:
	        ehloResponse == "502 Command not implemented\r\n"
	    when: "Sending TURN"
	        ehloResponse = ssWorker.handleMessage( "TURN ${domain}" )
	    then: 
	        ehloResponse == "502 Command not implemented\r\n"
	}
	
	def "test with a line containing two periods"() {
	    println "\n--- Starting test ${name.methodName}"
	    when:
            def serverName = "www.groovymail.org"
            def domain = "hot-groovy.com"
            def bString = "EHLO ${domain}${crlf}DATA${crlf}JJJ${crlf}" +
            "Hello\n..\nMore stuff${crlf}.${crlf}QUIT${crlf}"
            byte[] data = bString.getBytes()
    
            InputStream input = new ByteArrayInputStream( data )
            OutputStream output = new ByteArrayOutputStream() 
            
            def ssWorker = new ModularSMTPSocketWorker( input, output, serverName )
            ssWorker.doWork()
            
	    then:
	        output.toString() == "220 www.groovymail.org Simple Mail Transfer Service Ready\r\n" +
                "250-Hello hot-groovy.com\n" +
                "250 HELP\r\n" +
                "354 Start mail input; end with <CRLF>.<CRLF>\r\n" +
                "250 OK\r\n" +
                "221 www.groovymail.org Service closing transmission channel\r\n"
	    
	}
	
	def "test obsolete commands"() {
	    println "\n--- Starting test ${name.methodName}"
	    when:
            def serverName = "www.groovymail.org"
            def mIs = Mock( InputStream )
            def mOs = Mock( OutputStream )
            def domain = "hot-groovy.com"
            byte[] data = "EHLO ${domain}${crlf}SAML${crlf}SEND${crlf}SOML${crlf}TURN${crlf}QUIT${crlf}".getBytes()
            InputStream input = new ByteArrayInputStream( data )
            OutputStream output = new ByteArrayOutputStream() 
            new ModularSMTPSocketWorker( input, output, serverName ).doWork()
            
	    then:
            println "output to string: ++++\n${output.toString()}"
            println "++++ end of output"
            output.toString() == "220 www.groovymail.org Simple Mail Transfer Service Ready\r\n" + // opening
                "250-Hello hot-groovy.com\n" +
                "250 HELP\r\n" + // DATA
                "502 Command not implemented\r\n" + // SAML
                "502 Command not implemented\r\n" + // SEND
                "502 Command not implemented\r\n" + // SOML
                "502 Command not implemented\r\n" + // TURN
                "221 www.groovymail.org Service closing transmission channel\r\n" // QUIT
	}
	
	def "test common streams"() {
	    println "\n--- Starting test ${name.methodName}"
	    when:
            def serverName = "www.groovymail.org"
            def mIs = Mock( InputStream )
            def mOs = Mock( OutputStream )
            def domain = "hot-groovy.com"
            byte[] data = "EHLO ${domain}${crlf}DATA${crlf}JJJ${crlf}.${crlf}QUIT${crlf}".getBytes()
            InputStream input = new ByteArrayInputStream( data )
            OutputStream output = new ByteArrayOutputStream() 
            new ModularSMTPSocketWorker( input, output, serverName ).doWork()
            
	    then:
            println "output to string: ++++\n${output.toString()}"
            println "++++ end of output"
            output.toString() == "220 www.groovymail.org Simple Mail Transfer Service Ready\r\n" +
                "250-Hello hot-groovy.com\n" +
                "250 HELP\r\n" +
                "354 Start mail input; end with <CRLF>.<CRLF>\r\n" +
                "250 OK\r\n" +
                "221 www.groovymail.org Service closing transmission channel\r\n"
	}
	
	def "test common streams with reader mocking"() {
	    println "\n--- Starting test ${name.methodName}"
	    when:
            def serverName = "www.groovymail.org"
            def domain = "hot-groovy.com"
            byte[] data = "EHLO ${domain}${crlf}DATA${crlf}JJJ\nHHH${crlf}.${crlf}QUIT${crlf}".getBytes()
            InputStream input = new ByteArrayInputStream( data )
            OutputStream output = new ByteArrayOutputStream() 
            def ssWorker = new ModularSMTPSocketWorker( input, output, serverName )
            ssWorker.doWork()
            
	    then:
            println "output to string: ++++\n${output.toString()}"
            println "++++ end of output"
            output.toString() == "220 www.groovymail.org Simple Mail Transfer Service Ready\r\n" +
                "250-Hello hot-groovy.com\n" +
                "250 HELP\r\n" +
                "354 Start mail input; end with <CRLF>.<CRLF>\r\n" +
                "250 OK\r\n" +
                "221 www.groovymail.org Service closing transmission channel\r\n"
	    
	}
}

