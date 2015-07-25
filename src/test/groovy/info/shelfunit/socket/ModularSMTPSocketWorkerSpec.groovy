package info.shelfunit.socket

import spock.lang.Specification
// import spock.lang.Ignore
import java.io.InputStream
import java.io.OutputStream

import org.junit.Rule
import org.junit.rules.TestName

import info.shelfunit.mail.MailRunner
import info.shelfunit.socket.command.EHLOCommand

class ModularSMTPSocketWorkerSpec extends Specification {
    @Rule 
    TestName name = new TestName()
    
    def crlf = "\r\n"
    
    def setup() {
        println "\n--- Starting test ${name.methodName}"
    }          // run before every feature method
    def cleanup() {}        // run after every feature method
    def setupSpec() {
        MailRunner.runMetaProgramming()
        // ExpandoMetaClass.enableGlobally()
    }     // run before the first feature method
    def cleanupSpec() {}   // run after the last feature method
    
    

	def "test handling EHLO"() {
	    println "\n--- Starting test ${name.methodName}"
	    def serverName = "www.groovymail.org"
	    def ssWorker = new ModularSMTPSocketWorker( Mock( InputStream ), Mock( OutputStream ), serverName )
	    def ehloCommand = new EHLOCommand()
	    
	    expect:
	        ssWorker.serverName == "www.groovymail.org"
	    
	    def domain = "hot-groovy.com"
	    when:
	        def resultMap = ehloCommand.process( "EHLO ${domain}", [], [:] )
	        def ehloResponse = resultMap.resultString + "\r\n" // ssWorker.handleMessage(  )
	    then:
	        ehloResponse == "250-Hello ${domain}\n" +
	        "250 HELP\r\n"
	        resultMap.prevCommandSet == ["EHLO"]
	}
	
	def "test handling HELO"() {
	    def serverName = "www.groovymail.org"
	    def ssWorker = new ModularSMTPSocketWorker( Mock( InputStream ), Mock( OutputStream ), serverName )
	    def ehloCommand = new EHLOCommand()
	    
	    expect:
	        ssWorker.serverName == "www.groovymail.org"
	    
	    def domain = "hot-groovy.com"
	    when:
	        def resultMap = ehloCommand.process( "HELO ${domain}", [], [:] )
	        def ehloResponse = resultMap.resultString + "\r\n" // ssWorker.handleMessage(  )
	    then:
	        ehloResponse == "250 Hello ${domain}\r\n"
	        println "Here is the map: ${resultMap.prevCommandSet}"
	}
	
	def "test handling old commands"() {
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

