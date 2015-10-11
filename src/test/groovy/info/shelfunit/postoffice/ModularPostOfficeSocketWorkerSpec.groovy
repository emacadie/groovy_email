package info.shelfunit.postoffice

import spock.lang.Specification
// import spock.lang.Ignore
import java.io.InputStream
import java.io.OutputStream

import groovy.sql.Sql

import org.junit.Rule
import org.junit.rules.TestName

import info.shelfunit.mail.ConfigHolder
import info.shelfunit.mail.meta.MetaProgrammer
import info.shelfunit.smtp.command.EHLOCommand
import org.apache.shiro.crypto.hash.Sha512Hash

class ModularPostOfficeSocketWorkerSpec extends Specification {
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
        sql.execute "DELETE FROM email_user where username in ('gwashmps', 'jadamsmps', 'tee-jaymps')"
        sql.close()
    }   // run after the last feature method
    
    def addUsers() {
        def numIterations = 10000
        def salt = 'you say your password tastes like chicken? Add salt!'
        def atx512 = new Sha512Hash( 'somePassword', salt, numIterations )
        def params = [ 'gwashmps', ( new Sha512Hash( 'somePassword', 'gwashmps', numIterations ).toBase64() ), 'SHA-512', numIterations, 'George', 'Washington', 0 ]
        sql.execute 'insert into  email_user( username, password_hash, password_algo, iterations, first_name, last_name, version ) values ( ?, ?, ?, ?, ?, ?, ? )', params
        
        params = [ 'jadamsmps', ( new Sha512Hash( 'somePassword', 'jadamsmps', numIterations ).toBase64() ), 'SHA-512', numIterations, 'John', 'Adams', 0 ]
        sql.execute 'insert into  email_user( username, password_hash, password_algo, iterations, first_name, last_name, version ) values ( ?, ?, ?, ?, ?, ?, ? )', params
        
        params = [ 'tee-jaymps', ( new Sha512Hash( 'somePassword', 'tee-jaymps', numIterations ).toBase64() ), 'SHA-512', numIterations, 'Thomas', "Jefferson", 0 ]
        sql.execute 'insert into  email_user( username, password_hash, password_algo, iterations, first_name, last_name, version ) values ( ?, ?, ?, ?, ?, ?, ? )', params
        // sql.commit()
    }
    
    def addMessage( uuid, userName, messageString ) {
        def toAddress = "${userName}@${domainList[ 0 ]}".toString()
        def params = [ uuid, userName, 'hello@test.com', toAddress, messageString ]
        sql.execute 'insert into mail_store(id, username, from_address, to_address, text_body) values (?, ?, ?, ?, ?)', params
    }
    
    def "test basic session"() {
	    when:
            def domain = "hot-groovy.com"
            def bString = "USER gwashmps${crlf}" + 
            "PASS somePassword${crlf}" +
            "STAT${crlf}" +
            "QUIT${crlf}"
            byte[] data = bString.getBytes()
    
            InputStream input = new ByteArrayInputStream( data )
            OutputStream output = new ByteArrayOutputStream() 
            
            def ssWorker = new ModularPostOfficeSocketWorker( input, output, domainList ) // , sql )
            ssWorker.doWork()
            
	    then:
	        output.toString() == "+OK POP3 server ready <shelfunit.info>\r\n" +
                "+OK gwashmps is a valid mailbox\r\n" +
                "+OK gwashmps authenticated\r\n" +
                "+OK 0 null\r\n" +
                "+OK shelfunit.info POP3 server signing off\r\n"
	}

	 def "test one message"() {
	    when:
	        def theMess = "dkke" * 12
	        this.addMessage( UUID.randomUUID(), 'jadamsmps', theMess )
            def domain = "hot-groovy.com"
            def bString = "USER jadamsmps${crlf}" + 
            "PASS somePassword${crlf}" +
            "STAT${crlf}" +
            "RETR 1${crlf}" +
            "QUIT${crlf}"
            byte[] data = bString.getBytes()
    
            InputStream input = new ByteArrayInputStream( data )
            OutputStream output = new ByteArrayOutputStream() 
            
            def ssWorker = new ModularPostOfficeSocketWorker( input, output, domainList ) // , sql )
            ssWorker.doWork()
            
	    then:
	        output.toString() == "+OK POP3 server ready <shelfunit.info>\r\n" +
                "+OK jadamsmps is a valid mailbox\r\n" +
                "+OK jadamsmps authenticated\r\n" +
                "+OK 1 ${theMess.size()}\r\n" +
                "+OK ${theMess.size()} octets\r\n" +
                "${theMess}\r\n" +
                ".\r\n" +
                "+OK shelfunit.info POP3 server signing off\r\n"
	}
	
	def "test non-existent user"() {
	    when:
	        def theMess = "dkke" * 12
	        this.addMessage( UUID.randomUUID(), 'jadamsmps', theMess )
            def domain = "hot-groovy.com"
            def bString = "USER erer${crlf}" + 
            "PASS somePassword${crlf}" +
            "STAT${crlf}" +
            "QUIT${crlf}"
            byte[] data = bString.getBytes()
    
            InputStream input = new ByteArrayInputStream( data )
            OutputStream output = new ByteArrayOutputStream() 
            
            def ssWorker = new ModularPostOfficeSocketWorker( input, output, domainList ) // , sql )
            ssWorker.doWork()
            
	    then:
	        output.toString() == "+OK POP3 server ready <shelfunit.info>\r\n" +
	        "-ERR No such user erer\r\n" +
	        "-ERR Command not in proper form - No user sent\r\n" +
	        "-ERR Not in TRANSACTION state\r\n" +
	        "+OK shelfunit.info  POP3 server signing off\r\n"
	}
	
    /*
	def "test handling EHLO"() {
	    println "\n--- Starting test ${name.methodName}"
	    def ssWorker = new ModularPostOfficeSocketWorker( Mock( InputStream ), Mock( OutputStream ), domainList ) // , Mock( Sql ) )
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
	*/
	
	/*
	def "test handling HELO"() {
	    def ssWorker = new ModularPostOfficeSocketWorker( Mock( InputStream ), Mock( OutputStream ), domainList ) //, Mock( Sql ) )
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
	*/
	/*
	def "test handling old commands"() {
	    def ssWorker = new ModularPostOfficeSocketWorker( Mock( InputStream ), Mock( OutputStream ), domainList ) // , Mock( Sql ) )
	    
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
	*/
	/*
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
            
            def ssWorker = new ModularPostOfficeSocketWorker( input, output, domainList ) // , sql )
            ssWorker.doWork()
            
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
	*/
	/*
	def "test obsolete commands"() {
	    when:
            def mIs = Mock( InputStream )
            def mOs = Mock( OutputStream )
            def domain = "hot-groovy.com"
            byte[] data = "EHLO ${domain}${crlf}SAML${crlf}SEND${crlf}SOML${crlf}TURN${crlf}QUIT${crlf}".getBytes()
            InputStream input = new ByteArrayInputStream( data )
            OutputStream output = new ByteArrayOutputStream() 
            new ModularPostOfficeSocketWorker( input, output, domainList ).doWork()
            
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
	*/
	/*
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
            def msmtpw = new ModularPostOfficeSocketWorker( input, output, domainList )
            msmtpw.doWork()
            
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
	*/
	/*
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
            def ssWorker = new ModularPostOfficeSocketWorker( input, output, domainList )
            ssWorker.doWork()
            
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
	*/
}

