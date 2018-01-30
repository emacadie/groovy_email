package info.shelfunit.smtp

import spock.lang.Specification
import spock.lang.Ignore
import java.io.InputStream
import java.io.OutputStream

import org.junit.Rule
import org.junit.rules.TestName

import info.shelfunit.mail.ConfigHolder
import static info.shelfunit.mail.GETestUtils.addUser
import static info.shelfunit.mail.GETestUtils.getRandomString

import info.shelfunit.mail.meta.MetaProgrammer
import info.shelfunit.smtp.command.EHLOCommand

class ModularSMTPSocketWorkerSpec extends Specification {
    @Rule 
    TestName name = new TestName()
    
    def crlf = "\r\n"
    static sqlObject
    static domainList = [ 'shelfunit.info', 'groovy-is-groovy.org' ]
    static rString    = getRandomString()
    static gwString   = 'gw' + rString
    static jaString   = 'ja' + rString
    static tjString   = 'tj' + rString
    
    def setup() {
        println "\n--- Starting test ${name.methodName}"
    }          // run before every feature method
    
    def cleanup() {}        // run after every feature method
    
    def setupSpec() {
        MetaProgrammer.runMetaProgramming()
        ConfigHolder.instance.setConfObject( "src/test/resources/application.test.conf" )
        sqlObject = ConfigHolder.instance.getSqlObject() 
        this.addUsers()
    }     // run before the first feature method
    
    def cleanupSpec() {
        sqlObject.execute "DELETE FROM email_user where username in ( ?, ?, ? )", [ gwString, jaString, tjString ]
        sqlObject.execute "DELETE FROM mail_spool_in where from_address = ?", [ 'smtpnoauth@showboat.com' ]
        sqlObject.close()
    }   // run after the last feature method
    
    def addUsers() {
        addUser( sqlObject, 'George', 'Washington', gwString, 'somePassword' )
        addUser( sqlObject, 'John',   'Adams',      jaString, 'somePassword' )
        addUser( sqlObject, 'Jack',   "O'Neill",    tjString, 'somePassword' )
    }

	def "test handling EHLO"() {
	    println "\n--- Starting test ${name.methodName}"
	    def ssWorker    = new ModularSMTPSocketWorker( Mock( InputStream ), Mock( OutputStream ), domainList, '/10.178.98.210', 'groovy-email-is-awesome.com' ) 
	    def ehloCommand = new EHLOCommand()
	    
	    expect:
	        ssWorker.serverName == "shelfunit.info"
	    
	    def domain = "hot-groovy.com"
	    when:
	        def resultMap    = ehloCommand.process( "EHLO ${domain}", [], [:] )
	        def ehloResponse = resultMap.resultString + "\r\n" // ssWorker.handleMessage(  )
	    then:
	        ehloResponse == "250-Hello ${domain}\r\n" +
	        "250-8BITMIME\r\n" + 
	        "250-AUTH PLAIN\r\n" + 
	        "250 HELP\r\n"
	        resultMap.prevCommandSet == ["EHLO"]
	}
	
	def "test handling HELO"() {
	    def ssWorker    = new ModularSMTPSocketWorker( Mock( InputStream ), Mock( OutputStream ), domainList, '/10.178.98.210', 'groovy-email-is-awesome.com' ) 
	    def ehloCommand = new EHLOCommand()
	    
	    expect:
	        ssWorker.serverName == "shelfunit.info"
	    
	    def domain = "hot-groovy.com"
	    when:
	        def resultMap    = ehloCommand.process( "HELO ${domain}", [], [:] )
	        def ehloResponse = resultMap.resultString + "\r\n" // ssWorker.handleMessage(  )
	    then:
	        ehloResponse == "250 Hello ${domain}\r\n"
	        println "Here is the map: ${resultMap.prevCommandSet}"
	}
	
	def "test handling old commands"() {
	    def ssWorker = new ModularSMTPSocketWorker( Mock( InputStream ), Mock( OutputStream ), domainList, '/10.178.98.210', 'groovy-email-is-awesome.com' ) 
	    
	    expect:
	        ssWorker.serverName == "shelfunit.info"
	    
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

    def "test null message"() {
	    def ssWorker = new ModularSMTPSocketWorker( Mock( InputStream ), Mock( OutputStream ), domainList, '/10.178.98.210', 'groovy-email-is-awesome.com' ) 
	    
	    expect:
	        ssWorker.serverName == "shelfunit.info"
	    
	    def domain = "hot-groovy.com"
	    when: "Sending null"
	        def ehloResponse = ssWorker.handleMessage( null )
	    then:
	        ehloResponse == "250 OK\r\n"
    }
	
	def "test with a line containing two periods"() {
	    when:
            def domain  = "hot-groovy.com"
            def bString = "EHLO ${domain}${crlf}" + 
            "MAIL FROM:<smtpnoauth@showboat.com>${crlf}" +
            "RCPT TO:<${gwString}@shelfunit.info>${crlf}" +
            "DATA${crlf}JJJ${crlf}" +
            "Hello\n..\nMore stuff${crlf}.${crlf}QUIT${crlf}"
            byte[] data = bString.getBytes()
    
            InputStream input   = new ByteArrayInputStream( data )
            OutputStream output = new ByteArrayOutputStream() 
            
            def ssWorker = new ModularSMTPSocketWorker( input, output, domainList, '/10.178.98.210', 'groovy-email-is-awesome.com' ) 
            ssWorker.doWork()
            ssWorker.cleanup()
            
	    then:
	        output.toString() == "220 shelfunit.info Simple Mail Transfer Service Ready\r\n" +
                "250-Hello hot-groovy.com\r\n" +
                "250-8BITMIME\r\n" +
                "250-AUTH PLAIN\r\n" + 
                "250 HELP\r\n" +
                "250 OK\r\n" +
                "250 OK\r\n" +
                "354 Start mail input; end with <CRLF>.<CRLF>\r\n" +
                "250 OK\r\n" +
                "221 shelfunit.info Service closing transmission channel\r\n"
	    
	}
	
	def "test obsolete commands"() {
	    when:
            def mIs    = Mock( InputStream )
            def mOs    = Mock( OutputStream )
            def domain = "hot-groovy.com"
            byte[] data         = "EHLO ${domain}${crlf}SAML${crlf}SEND${crlf}SOML${crlf}TURN${crlf}QUIT${crlf}".getBytes()
            InputStream input   = new ByteArrayInputStream( data )
            OutputStream output = new ByteArrayOutputStream() 
            def worker = new ModularSMTPSocketWorker( input, output, domainList, '/10.1.2.3', 'groovy-email-is-awesome.com' ) 
            worker.doWork()
            worker.cleanup()
            
	    then:
            println "output to string: ++++\n${output.toString()}"
            println "++++ end of output"
            output.toString() == "220 shelfunit.info Simple Mail Transfer Service Ready\r\n" + // opening
                "250-Hello hot-groovy.com\r\n" +
                "250-8BITMIME\r\n"   +
                "250-AUTH PLAIN\r\n" + 
                "250 HELP\r\n" + // DATA
                "502 Command not implemented\r\n" + // SAML
                "502 Command not implemented\r\n" + // SEND
                "502 Command not implemented\r\n" + // SOML
                "502 Command not implemented\r\n" + // TURN
                "221 shelfunit.info Service closing transmission channel\r\n" // QUIT
	}

	def "test common streams"() {
	    when:
            def mIs    = Mock( InputStream )
            def mOs    = Mock( OutputStream )
            def domain = "hot-groovy.com"
            
            def dataString = "EHLO ${domain}${crlf}"      +
            "MAIL FROM:<smtpnoauth@showboat.com>${crlf}"  +
            "RCPT TO:<${gwString}@shelfunit.info>${crlf}" +
            "DATA${crlf}"  +
            "JJJ${crlf}.${crlf}" +
            "QUIT${crlf}"
            
            byte[] data         = dataString.getBytes()
            InputStream input   = new ByteArrayInputStream( data )
            OutputStream output = new ByteArrayOutputStream() 
            def msmtpw          = new ModularSMTPSocketWorker( input, output, domainList, '/10.178.98.210', 'groovy-email-is-awesome.com' ) 
            msmtpw.doWork()
            msmtpw.cleanup()
            
	    then:
            println "output to string: ++++\n${output.toString()}"
            println "++++ end of output"
            output.toString() == "220 shelfunit.info Simple Mail Transfer Service Ready${crlf}"  +
               "250-Hello hot-groovy.com${crlf}"  +
               "250-8BITMIME${crlf}"  +
               "250-AUTH PLAIN${crlf}"  +
               "250 HELP${crlf}"  +
               "250 OK${crlf}"  +
               "250 OK${crlf}"  +
               "354 Start mail input; end with <CRLF>.<CRLF>${crlf}"  +
               "250 OK${crlf}"  +
               "221 shelfunit.info Service closing transmission channel${crlf}" 
	}
	
	def "test common streams with reader mocking"() {
	    when:
            def domain     = "hot-groovy.com"
            def dataString = "EHLO ${domain}${crlf}" + 
            "MAIL FROM:<smtpnoauth@showboat.com>${crlf}" +
            "RCPT TO:<${gwString}@shelfunit.info>${crlf}" +
            "DATA${crlf}JJJ\nHHH${crlf}.${crlf}QUIT${crlf}"
            byte[] data         = dataString.getBytes()
            InputStream input   = new ByteArrayInputStream( data )
            OutputStream output = new ByteArrayOutputStream() 
            def ssWorker        = new ModularSMTPSocketWorker( input, output, domainList, '/10.178.98.210', 'groovy-email-is-awesome.com' ) 
            ssWorker.doWork()
            ssWorker.cleanup()
            
	    then:
            println "output to string: ++++\n${output.toString()}"
            println "++++ end of output"
            output.toString() == "220 shelfunit.info Simple Mail Transfer Service Ready\r\n" +
                "250-Hello hot-groovy.com\r\n" +
                "250-8BITMIME\r\n" +
                "250-AUTH PLAIN\r\n" + 
                "250 HELP\r\n" +
                "250 OK\r\n" +
                "250 OK\r\n" +
                "354 Start mail input; end with <CRLF>.<CRLF>\r\n" +
                "250 OK\r\n" +
                "221 shelfunit.info Service closing transmission channel\r\n"
	}

    @Ignore
	def "test common streams with empty message"() {
	    when:
            def mIs    = Mock( InputStream )
            def mOs    = Mock( OutputStream )
            def domain = "hot-groovy.com"
            
            def dataString = null + "${crlf}EHLO ${domain}${crlf}"  +
            "MAIL FROM:<smtpnoauth@showboat.com>${crlf}" +
            "RCPT TO:<${gwString}@shelfunit.info>${crlf}" +
            "DATA${crlf}"  +
            "JJJ${crlf}.${crlf}" +
            "QUIT${crlf}"
            
            byte[] data         = dataString.getBytes()
            InputStream input   = new ByteArrayInputStream( data )
            OutputStream output = new ByteArrayOutputStream() 
            def msmtpw          = new ModularSMTPSocketWorker( input, output, domainList, '/10.178.98.210', 'groovy-email-is-awesome.com' ) 
            msmtpw.doWork()
            msmtpw.cleanup()
            
	    then:
            println "output to string: ++++\n${output.toString()}"
            println "++++ end of output"
            output.toString() == "250 OK${crlf}" +
                "220 shelfunit.info Simple Mail Transfer Service Ready${crlf}"  +
                "250 OK${crlf}" +
                "250-Hello hot-groovy.com${crlf}"  +
                "250-8BITMIME${crlf}"   +
                "250-AUTH PLAIN${crlf}" +
                "250 HELP${crlf}"       +
                "250 OK${crlf}"         +
                "250 OK${crlf}"         +
                "354 Start mail input; end with <CRLF>.<CRLF>${crlf}"  +
                "250 OK${crlf}"  +
                "221 shelfunit.info Service closing transmission channel${crlf}" 
	}

    @Ignore
    def "test common streams with one empty message"() {
	    when:
            def mIs    = Mock( InputStream )
            def mOs    = Mock( OutputStream )
            def domain = "hot-groovy.com"
            
            def dataString = null + "${crlf}QUIT${crlf}"
            
            byte[] data         = dataString.getBytes()
            InputStream input   = new ByteArrayInputStream( data )
            OutputStream output = new ByteArrayOutputStream() 
            def msmtpw          = new ModularSMTPSocketWorker( input, output, domainList, '/10.178.98.210', 'groovy-email-is-awesome.com' ) 
            msmtpw.doWork()
            msmtpw.cleanup()
            
	    then:
            println "output to string: ++++\n${output.toString()}"
            println "++++ end of output"
            output.toString() == "250 OK${crlf}" +
                "220 shelfunit.info Simple Mail Transfer Service Ready${crlf}"  +
                "250 OK${crlf}" +
                "250-Hello hot-groovy.com${crlf}"  +
                "250-8BITMIME${crlf}"   +
                "250-AUTH PLAIN${crlf}" +
                "250 HELP${crlf}"       +
                "250 OK${crlf}"         +
                "250 OK${crlf}"         +
                "354 Start mail input; end with <CRLF>.<CRLF>${crlf}"  +
                "250 OK${crlf}"  +
                "221 shelfunit.info Service closing transmission channel${crlf}" 
	}

} // line 246

