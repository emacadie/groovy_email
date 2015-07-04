package info.shelfunit.socket

import spock.lang.Specification
// import spock.lang.Ignore
import java.io.InputStream
import java.io.OutputStream

import org.junit.Rule
import org.junit.rules.TestName

import info.shelfunit.mail.MailRunner
import info.shelfunit.socket.command.EHLOCommand

import org.xbill.DNS.Address

import groovy.mock.interceptor.StubFor

class EHLOCommandSpec extends Specification {
    
    def crlf = "\r\n"

    @Rule 
    TestName name = new TestName()
    
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
	    def serverName = "www.groovymail.org"
	    def ehloCommand = new EHLOCommand()
	    
	    def domain = "hot-groovy.com"
	    when:
	        def resultMap = ehloCommand.process( "EHLO ${domain}", [], [:] )
	        def ehloResponse = resultMap.resultString + "\r\n" 
	    then:
	        ehloResponse == "250-Hello ${domain}\n" +
	        "250 HELP\r\n"
	        resultMap.prevCommandList == ["EHLO"]
	}
	
	def "test handling HELO"() {
	    def serverName = "www.groovymail.org"
	    def ehloCommand = new EHLOCommand()
	    
	    def domain = "hot-groovy.com"
	    when:
	        def resultMap = ehloCommand.process( "HELO ${domain}", [], [:] )
	        def ehloResponse = resultMap.resultString + "\r\n" 
	    then:
	        ehloResponse == "250 Hello ${domain}\r\n"
	}
	
	def "test handling domain more than 255 char"() {
	    def serverName = "www.groovymail.org"
	    def ehloCommand = new EHLOCommand()
	    
	    def longString = ( "f" * 252 ) + '.com'
	    when:
	        def resultMap = ehloCommand.process( "EHLO ${longString}", [], [:] )
	        def ehloResponse = resultMap.resultString + "\r\n" 
	    then:
	        ehloResponse == "501 Domain name length beyond 255 char limit per RFC 3696\r\n"
	}
	
	def "test that command list and buffer map are cleared"() {
	    def domain = "www.groovymail.org"
	    def ehloCommand = new EHLOCommand()
	    
	    when:
	        def prevCommandList = [ 'Get the hell off my ship', 'keep us under the radar, Wash' ]
	        def bufferMap = [ name:'Jayne', location: 'bunk' ]
	    then:
	        prevCommandList.size() == 2
	        bufferMap.size() == 2
	    when:
	        def resultMap = ehloCommand.process( "EHLO ${domain}", prevCommandList, bufferMap )
	        def ehloResponse = resultMap.resultString + "\r\n" 
	        def newList = resultMap.prevCommandList
	        def newMap = resultMap.bufferMap
	    then:
	        ehloResponse == "250-Hello ${domain}\n" +
	        "250 HELP\r\n"
	        newList.size() == 1
	        newList[ 0 ] == "EHLO"
	        newMap.size() == 0
	        
	}
	/*
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
	*/
	/*
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
	*/
	/*
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
	*/
	/*
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
	*/
	/*
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
	*/
	
	def "test getting address"() {
	    def serverName = "www.groovymail.org"
	    def ehloCommand = new EHLOCommand()
	    def inetAddress = Stub( InetAddress )
	    def address = GroovyStub( Address )
	    address.getByName(_) >> inetAddress
	    inetAddress.hostAddress >> 'X.X.X.X'
	    def result = ehloCommand.processDomain( "XYZ" )
	    println "here is result: ${result}"
	    expect:
	        result == 'X.X.X.X'
	        1 == 1
	}
	
	def "test without mocks or stubs"() {
	    
	    when:
            def longString = ( "f" * 256 ) + '.com'
            def serverName = "www.groovymail.org"
            def ehloCommand = new EHLOCommand()
            def result = ehloCommand.processDomain( 'www.shelfunit.info' )
            println "here is result: ${result}"
	    then:
	        result == '45.33.18.182'
	}
	
	// this works even if not connected to internet
	def "test good domain with mocks or stubs"() {
	    when:
            def longString = ( "f" * 256 ) + '.com'
            def serverName = "www.groovymail.org"
            def ehloCommand = new EHLOCommand()
            def inetAddress = GroovyStub( InetAddress )
            inetAddress.hostAddress >> '45.33.18.182'
            def result = ''
            def stub = new StubFor( Address )      
            stub.demand.with {                  
                getByName{ inetAddress }
            }
            stub.use {                          
                result = ehloCommand.processDomain( 'www.shelfunit.info' )
            }
            println "here is result: ${result}"
	    then:
	        stub.expect.verify() 
	        result == '45.33.18.182'
	}
}

