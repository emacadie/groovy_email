package info.shelfunit.socket

import spock.lang.Specification
// import spock.lang.Ignore
import java.io.InputStream
import java.io.OutputStream

import groovy.sql.Sql

import org.junit.Rule
import org.junit.rules.TestName

import info.shelfunit.mail.MetaProgrammer
import info.shelfunit.socket.command.EHLOCommand
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
        def db = [ url: "jdbc:postgresql://${System.properties[ 'host_and_port' ]}/${System.properties[ 'dbname' ]}",
        user: System.properties[ 'dbuser' ], password: System.properties[ 'dbpassword' ], driver: 'org.postgresql.Driver' ]
        sql = Sql.newInstance( db.url, db.user, db.password, db.driver )
        this.addUsers()
    }     // run before the first feature method
    def cleanupSpec() {
        sql.execute "DELETE FROM email_user"
        sql.close()
    }   // run after the last feature method
    
    def addUsers() {
        def numIterations = 10000
        def salt = 'you say your password tastes like chicken? Add salt!'
        def atx512 = new Sha512Hash( 'somePassword', salt, 1000000 )
        def params = [ 'gwash', atx512.toBase64(), 'SHA-512', numIterations, 'George', 'Washington', 0 ]
        sql.execute 'insert into  email_user( username, password_hash, password_algo, iterations, first_name, last_name, version ) values ( ?, ?, ?, ?, ?, ?, ? )', params
        
        params = [ 'jadams', atx512.toBase64(), 'SHA-512', numIterations, 'John', 'Adams', 0 ]
        sql.execute 'insert into  email_user( username, password_hash, password_algo, iterations, first_name, last_name, version ) values ( ?, ?, ?, ?, ?, ?, ? )', params
        
        params = [ 'tee-jay', atx512.toBase64(), 'SHA-512', numIterations, 'Thomas', "Jefferson", 0 ]
        sql.execute 'insert into  email_user( username, password_hash, password_algo, iterations, first_name, last_name, version ) values ( ?, ?, ?, ?, ?, ?, ? )', params
        // sql.commit()
    }

	def "test handling EHLO"() {
	    println "\n--- Starting test ${name.methodName}"
	    def ssWorker = new ModularSMTPSocketWorker( Mock( InputStream ), Mock( OutputStream ), domainList, Mock( Sql ) )
	    def ehloCommand = new EHLOCommand()
	    
	    expect:
	        ssWorker.serverName == "shelfunit.info"
	    
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
	    def ssWorker = new ModularSMTPSocketWorker( Mock( InputStream ), Mock( OutputStream ), domainList, Mock( Sql ) )
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
	    def ssWorker = new ModularSMTPSocketWorker( Mock( InputStream ), Mock( OutputStream ), domainList, Mock( Sql ) )
	    
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
            def bString = "EHLO ${domain}${crlf}DATA${crlf}JJJ${crlf}" +
            "Hello\n..\nMore stuff${crlf}.${crlf}QUIT${crlf}"
            byte[] data = bString.getBytes()
    
            InputStream input = new ByteArrayInputStream( data )
            OutputStream output = new ByteArrayOutputStream() 
            
            def ssWorker = new ModularSMTPSocketWorker( input, output, domainList, sql )
            ssWorker.doWork()
            
	    then:
	        output.toString() == "220 shelfunit.info Simple Mail Transfer Service Ready\r\n" +
                "250-Hello hot-groovy.com\n" +
                "250 HELP\r\n" +
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
            new ModularSMTPSocketWorker( input, output, domainList, sql ).doWork()
            
	    then:
            println "output to string: ++++\n${output.toString()}"
            println "++++ end of output"
            output.toString() == "220 shelfunit.info Simple Mail Transfer Service Ready\r\n" + // opening
                "250-Hello hot-groovy.com\n" +
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
            byte[] data = "EHLO ${domain}${crlf}DATA${crlf}JJJ${crlf}.${crlf}QUIT${crlf}".getBytes()
            InputStream input = new ByteArrayInputStream( data )
            OutputStream output = new ByteArrayOutputStream() 
            new ModularSMTPSocketWorker( input, output, domainList, sql ).doWork()
            
	    then:
            println "output to string: ++++\n${output.toString()}"
            println "++++ end of output"
            output.toString() == "220 shelfunit.info Simple Mail Transfer Service Ready\r\n" +
                "250-Hello hot-groovy.com\n" +
                "250 HELP\r\n" +
                "354 Start mail input; end with <CRLF>.<CRLF>\r\n" +
                "250 OK\r\n" +
                "221 shelfunit.info Service closing transmission channel\r\n"
	}
	
	def "test common streams with reader mocking"() {
	    when:
            def domain = "hot-groovy.com"
            byte[] data = "EHLO ${domain}${crlf}DATA${crlf}JJJ\nHHH${crlf}.${crlf}QUIT${crlf}".getBytes()
            InputStream input = new ByteArrayInputStream( data )
            OutputStream output = new ByteArrayOutputStream() 
            def ssWorker = new ModularSMTPSocketWorker( input, output, domainList, sql )
            ssWorker.doWork()
            
	    then:
            println "output to string: ++++\n${output.toString()}"
            println "++++ end of output"
            output.toString() == "220 shelfunit.info Simple Mail Transfer Service Ready\r\n" +
                "250-Hello hot-groovy.com\n" +
                "250 HELP\r\n" +
                "354 Start mail input; end with <CRLF>.<CRLF>\r\n" +
                "250 OK\r\n" +
                "221 shelfunit.info Service closing transmission channel\r\n"
	}
}

