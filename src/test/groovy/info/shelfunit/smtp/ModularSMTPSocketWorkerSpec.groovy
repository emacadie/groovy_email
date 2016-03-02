package info.shelfunit.smtp

import spock.lang.Specification
// import spock.lang.Ignore
import java.io.InputStream
import java.io.OutputStream

import groovy.sql.Sql

import org.junit.Rule
import org.junit.rules.TestName

import info.shelfunit.mail.ConfigHolder
import static info.shelfunit.mail.GETestUtils.getBase64Hash
import info.shelfunit.mail.meta.MetaProgrammer
import info.shelfunit.smtp.command.EHLOCommand
import org.apache.shiro.crypto.hash.Sha512Hash

class ModularSMTPSocketWorkerSpec extends Specification {
    @Rule 
    TestName name = new TestName()
    
    def crlf = "\r\n"
    static sql
    static domainList = [ 'shelfunit.info', 'groovy-is-groovy.org' ]
    
    def setup() {
        println "\n--- Starting test ${name.methodName}"
    }          // run before every feature method
    
    def cleanup() {}        // run after every feature method
    
    def setupSpec() {
        MetaProgrammer.runMetaProgramming()
        ConfigHolder.instance.setConfObject( "src/test/resources/application.test.conf" )
        def conf = ConfigHolder.instance.getConfObject()
        def db = ConfigHolder.instance.returnDbMap() 
        sql = Sql.newInstance( db.url, db.user, db.password, db.driver )
        this.addUsers()
    }     // run before the first feature method
    
    def cleanupSpec() {
        sql.execute "DELETE FROM email_user where username in ('gwash', 'jadams', 'tee-jay')"
        sql.close()
    }   // run after the last feature method
    
    def addUsers() {
        def numIterations = 10000
        def salt = 'you say your password tastes like chicken? Add salt!'
        def atx512 = new Sha512Hash( 'somePassword', salt, numIterations )
        def params = [ 'gwash', atx512.toBase64(), 'SHA-512', numIterations, getBase64Hash( 'gwash', 'somePassword' ), 'George', 'Washington', 0 ]
        sql.execute 'insert into  email_user( username, password_hash, password_algo, iterations, base_64_hash, first_name, last_name, version ) values ( ?, ?, ?, ?, ?, ?, ?, ? )', params
        
        params = [ 'jadams', atx512.toBase64(), 'SHA-512', numIterations, getBase64Hash( 'jadams', 'somePassword' ), 'John', 'Adams', 0 ]
        sql.execute 'insert into  email_user( username, password_hash, password_algo, iterations, base_64_hash, first_name, last_name, version ) values ( ?, ?, ?, ?, ?, ?, ?, ? )', params
        
        params = [ 'tee-jay', atx512.toBase64(), 'SHA-512', numIterations, getBase64Hash( 'tee-jay', 'somePassword' ), 'Thomas', "Jefferson", 0 ]
        sql.execute 'insert into  email_user( username, password_hash, password_algo, iterations, base_64_hash, first_name, last_name, version ) values ( ?, ?, ?, ?, ?, ?, ?, ? )', params
        // sql.commit()
    }

	def "test handling EHLO"() {
	    println "\n--- Starting test ${name.methodName}"
	    def ssWorker = new ModularSMTPSocketWorker( Mock( InputStream ), Mock( OutputStream ), domainList ) // , Mock( Sql ) )
	    def ehloCommand = new EHLOCommand()
	    
	    expect:
	        ssWorker.serverName == "shelfunit.info"
	    
	    def domain = "hot-groovy.com"
	    when:
	        def resultMap = ehloCommand.process( "EHLO ${domain}", [], [:] )
	        def ehloResponse = resultMap.resultString + "\r\n" // ssWorker.handleMessage(  )
	    then:
	        ehloResponse == "250-Hello ${domain}\r\n" +
	        "250-8BITMIME\r\n" + 
	        "250 HELP\r\n"
	        resultMap.prevCommandSet == ["EHLO"]
	}
	
	def "test handling HELO"() {
	    def ssWorker = new ModularSMTPSocketWorker( Mock( InputStream ), Mock( OutputStream ), domainList ) //, Mock( Sql ) )
	    def ehloCommand = new EHLOCommand()
	    
	    expect:
	        ssWorker.serverName == "shelfunit.info"
	    
	    def domain = "hot-groovy.com"
	    when:
	        def resultMap = ehloCommand.process( "HELO ${domain}", [], [:] )
	        def ehloResponse = resultMap.resultString + "\r\n" // ssWorker.handleMessage(  )
	    then:
	        ehloResponse == "250 Hello ${domain}\r\n"
	        println "Here is the map: ${resultMap.prevCommandSet}"
	}
	
	def "test handling old commands"() {
	    def ssWorker = new ModularSMTPSocketWorker( Mock( InputStream ), Mock( OutputStream ), domainList ) // , Mock( Sql ) )
	    
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
	
	def "test with a line containing two periods"() {
	    when:
            def domain = "hot-groovy.com"
            def bString = "EHLO ${domain}${crlf}" + 
            "MAIL FROM:<aaa@showboat.com>${crlf}" +
            "RCPT TO:<gwash@shelfunit.info>${crlf}" +
            "DATA${crlf}JJJ${crlf}" +
            "Hello\n..\nMore stuff${crlf}.${crlf}QUIT${crlf}"
            byte[] data = bString.getBytes()
    
            InputStream input = new ByteArrayInputStream( data )
            OutputStream output = new ByteArrayOutputStream() 
            
            def ssWorker = new ModularSMTPSocketWorker( input, output, domainList ) // , sql )
            ssWorker.doWork()
            ssWorker.cleanup()
            
	    then:
	        output.toString() == "220 shelfunit.info Simple Mail Transfer Service Ready\r\n" +
                "250-Hello hot-groovy.com\r\n" +
                "250-8BITMIME\r\n" +
                "250 HELP\r\n" +
                "250 OK\r\n" +
                "250 OK\r\n" +
                "354 Start mail input; end with <CRLF>.<CRLF>\r\n" +
                "250 OK\r\n" +
                "221 shelfunit.info Service closing transmission channel\r\n"
	    
	}
	
	def "test obsolete commands"() {
	    when:
            def mIs = Mock( InputStream )
            def mOs = Mock( OutputStream )
            def domain = "hot-groovy.com"
            byte[] data = "EHLO ${domain}${crlf}SAML${crlf}SEND${crlf}SOML${crlf}TURN${crlf}QUIT${crlf}".getBytes()
            InputStream input = new ByteArrayInputStream( data )
            OutputStream output = new ByteArrayOutputStream() 
            def worker = new ModularSMTPSocketWorker( input, output, domainList )
            worker.doWork()
            worker.cleanup()
            
	    then:
            println "output to string: ++++\n${output.toString()}"
            println "++++ end of output"
            output.toString() == "220 shelfunit.info Simple Mail Transfer Service Ready\r\n" + // opening
                "250-Hello hot-groovy.com\r\n" +
                "250-8BITMIME\r\n" +
                "250 HELP\r\n" + // DATA
                "502 Command not implemented\r\n" + // SAML
                "502 Command not implemented\r\n" + // SEND
                "502 Command not implemented\r\n" + // SOML
                "502 Command not implemented\r\n" + // TURN
                "221 shelfunit.info Service closing transmission channel\r\n" // QUIT
	}

	def "test common streams"() {
	    when:
            def mIs = Mock( InputStream )
            def mOs = Mock( OutputStream )
            def domain = "hot-groovy.com"
            
            def dataString = "EHLO ${domain}${crlf}"  +
            "MAIL FROM:<aaa@showboat.com>${crlf}" +
            "RCPT TO:<gwash@shelfunit.info>${crlf}" +
            "DATA${crlf}"  +
            "JJJ${crlf}.${crlf}" +
            "QUIT${crlf}"
            
            byte[] data = dataString.getBytes()
            InputStream input = new ByteArrayInputStream( data )
            OutputStream output = new ByteArrayOutputStream() 
            def msmtpw = new ModularSMTPSocketWorker( input, output, domainList )
            msmtpw.doWork()
            msmtpw.cleanup()
            
	    then:
            println "output to string: ++++\n${output.toString()}"
            println "++++ end of output"
            output.toString() == "220 shelfunit.info Simple Mail Transfer Service Ready\r\n" +
                "250-Hello hot-groovy.com\r\n" +
                "250-8BITMIME\r\n" + 
                "250 HELP\r\n" +
                "250 OK\r\n" +
                "250 OK\r\n" +
                "354 Start mail input; end with <CRLF>.<CRLF>\r\n" +
                "250 OK\r\n" +
                "221 shelfunit.info Service closing transmission channel\r\n"
                
	}
	
	def "test common streams with reader mocking"() {
	    when:
            def domain = "hot-groovy.com"
            def dataString = "EHLO ${domain}${crlf}" + 
            "MAIL FROM:<aaa@showboat.com>${crlf}" +
            "RCPT TO:<gwash@shelfunit.info>${crlf}" +
            "DATA${crlf}JJJ\nHHH${crlf}.${crlf}QUIT${crlf}"
            byte[] data = dataString.getBytes()
            InputStream input = new ByteArrayInputStream( data )
            OutputStream output = new ByteArrayOutputStream() 
            def ssWorker = new ModularSMTPSocketWorker( input, output, domainList )
            ssWorker.doWork()
            ssWorker.cleanup()
            
	    then:
            println "output to string: ++++\n${output.toString()}"
            println "++++ end of output"
            output.toString() == "220 shelfunit.info Simple Mail Transfer Service Ready\r\n" +
                "250-Hello hot-groovy.com\r\n" +
                "250-8BITMIME\r\n" +
                "250 HELP\r\n" +
                "250 OK\r\n" +
                "250 OK\r\n" +
                "354 Start mail input; end with <CRLF>.<CRLF>\r\n" +
                "250 OK\r\n" +
                "221 shelfunit.info Service closing transmission channel\r\n"
	}
}

