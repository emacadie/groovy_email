package info.shelfunit.postoffice

import spock.lang.Specification
// import spock.lang.Ignore
import java.io.InputStream
import java.io.OutputStream

import org.junit.Rule
import org.junit.rules.TestName

import info.shelfunit.mail.ConfigHolder
import info.shelfunit.mail.meta.MetaProgrammer
import static info.shelfunit.mail.GETestUtils.getRandomString
import static info.shelfunit.mail.GETestUtils.addUser
import static info.shelfunit.mail.GETestUtils.addMessage
import info.shelfunit.smtp.command.EHLOCommand

class ModularPostOfficeSocketWorkerSpec extends Specification {
    @Rule 
    TestName name = new TestName()
    
    def crlf = "\r\n"
    static sql
    static rString    = getRandomString()
    static gwString   = 'gw' + rString
    static jaString   = 'ja' + rString
    static tjString   = 'tj' + rString
    static domainList = [ 'shelfunit.info', 'groovy-is-groovy.org' ]
    
    def setup() {
        println "\n--- Starting test ${name.methodName}"
    }          // run before every feature method
    
    def cleanup() {}        // run after every feature method
    
    def setupSpec() {
        MetaProgrammer.runMetaProgramming()
        ConfigHolder.instance.setConfObject( "src/test/resources/application.test.conf" )
        sql = ConfigHolder.instance.getSqlObject() 
        this.addUsers()
    }     // run before the first feature method
    
    def cleanupSpec() {
        sql.execute "DELETE FROM email_user where username in ( ?, ?, ? )", [ gwString, jaString, tjString ]
        sql.close()
    }   // run after the last feature method
    
    def addUsers() {
        addUser( sql, 'George', 'Washington', gwString, 'somePassword' )
        addUser( sql, 'John', 'Adams', jaString, 'somePassword' )
        addUser( sql, 'Jack', "O'Neill", tjString, 'somePassword' )
    }
    
    def "test basic session"() {
        
	    when:
            def domain = "hot-groovy.com"
            def bString = "USER ${gwString}${crlf}" + 
            "PASS somePassword${crlf}" +
            "STAT${crlf}" +
            "QUIT${crlf}"
            byte[] data = bString.getBytes()
    
            InputStream input = new ByteArrayInputStream( data )
            OutputStream output = new ByteArrayOutputStream() 
            
            def ssWorker = new ModularPostOfficeSocketWorker( input, output, domainList ) // , sql )
            ssWorker.doWork()
            ssWorker.cleanup()
            
	    then:
	        output.toString() == "+OK POP3 server ready <shelfunit.info>\r\n" +
                "+OK ${gwString} is a valid mailbox\r\n" +
                "+OK ${gwString} authenticated\r\n" +
                "+OK 0 null\r\n" +
                "+OK shelfunit.info POP3 server signing off\r\n"
	}

	 def "test one message"() {
	    when:
	        def theMess = "dkke" * 12
	        addMessage( sql, UUID.randomUUID(), jaString, theMess, domainList[ 0 ] )
            def domain = "hot-groovy.com"
            def bString = "USER ${jaString}${crlf}" + 
            "PASS somePassword${crlf}" +
            "STAT${crlf}" +
            "RETR 1${crlf}" +
            "QUIT${crlf}"
            byte[] data = bString.getBytes()
    
            InputStream input = new ByteArrayInputStream( data )
            OutputStream output = new ByteArrayOutputStream() 
            
            def ssWorker = new ModularPostOfficeSocketWorker( input, output, domainList ) // , sql )
            ssWorker.doWork()
            ssWorker.cleanup()
            
	    then:
	        output.toString() == "+OK POP3 server ready <shelfunit.info>\r\n" +
                "+OK ${jaString} is a valid mailbox\r\n" +
                "+OK ${jaString} authenticated\r\n" +
                "+OK 1 ${theMess.size()}\r\n" +
                "+OK ${theMess.size()} octets\r\n" +
                "${theMess}\r\n" +
                ".\r\n" +
                "+OK shelfunit.info POP3 server signing off\r\n"
	}
	
	def "test non-existent user"() {
	    when:
	        def theMess = "dkke" * 12
	        addMessage( sql, UUID.randomUUID(), jaString, theMess, domainList[ 0 ] )
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
            ssWorker.cleanup()
            
	    then:
	        output.toString() == "+OK POP3 server ready <shelfunit.info>\r\n" +
	        "-ERR No such user erer\r\n" +
	        "-ERR Command not in proper form - No user sent\r\n" +
	        "-ERR Not in TRANSACTION state\r\n" +
	        "+OK shelfunit.info  POP3 server signing off\r\n"
	}

} // line 338

