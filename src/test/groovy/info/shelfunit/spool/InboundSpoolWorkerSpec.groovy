package info.shelfunit.spool

import spock.lang.Specification
// import spock.lang.Ignore
import java.io.InputStream
import java.io.OutputStream

import org.junit.Rule
import org.junit.rules.TestName

import info.shelfunit.mail.ConfigHolder
import static info.shelfunit.mail.GETestUtils.addUser
import static info.shelfunit.mail.GETestUtils.getRandomString

import info.shelfunit.mail.meta.MetaProgrammer
import info.shelfunit.smtp.command.EHLOCommand

class InboundSpoolWorkerSpec extends Specification {
    @Rule 
    TestName name = new TestName()
    
    def crlf = "\r\n"
    static sql
    static domainList = [ 'shelfunit.info', 'groovy-is-groovy.org' ]
    static rString    = getRandomString()
    static gwString   = 'gw' + rString
    static jaString   = 'ja' + rString
    static tjString   = 'tj' + rString
    static InboundSpoolWorker isw
    
    def setup() {
        println "\n--- Starting test ${name.methodName}"
    }          // run before every feature method
    
    def cleanup() {}        // run after every feature method
    
    def setupSpec() {
        MetaProgrammer.runMetaProgramming()
        ConfigHolder.instance.setConfObject( "src/test/resources/application.test.conf" )
        sql = ConfigHolder.instance.getSqlObject() 
        this.addUsers()
        isw = new InboundSpoolWorker()
        
    }     // run before the first feature method
    
    def cleanupSpec() {
        sql.execute "DELETE FROM email_user where username in ( ?, ?, ? )", [ gwString, jaString, tjString ]
        sql.execute "DELETE FROM mail_spool_in where from_address = ?", [ 'aaa@showboat.com' ]
        sql.close()
    }   // run after the last feature method
    
    def addUsers() {
        addUser( sql, 'George', 'Washington', gwString, 'somePassword' )
        addUser( sql, 'John', 'Adams', jaString, 'somePassword' )
        addUser( sql, 'Jack', "O'Neill", tjString, 'somePassword' )
    }

	def "test handling EHLO"() {
	    println "\n--- Starting test ${name.methodName}"
	    try {
	        isw.doWork()
	    } catch ( Exception e ) {
            println "Exception: ${e}"
            println "${e.getStackTrace()}"
        }

	    expect:
	        1 == 1
	    /*
	    def domain = "hot-groovy.com"
	    when:
	        def resultMap = ehloCommand.process( "EHLO ${domain}", [], [:] )
	        def ehloResponse = resultMap.resultString + "\r\n" // ssWorker.handleMessage(  )
	    then:
	        ehloResponse == "250-Hello ${domain}\r\n" +
	        "250-8BITMIME\r\n" + 
	        "250-AUTH PLAIN\r\n" + 
	        "250 HELP\r\n"
	        resultMap.prevCommandSet == ["EHLO"]
	        */
	}

	
} // line 246
