package info.shelfunit.smtp.command

// import spock.lang.Ignore
import spock.lang.Specification

import org.junit.Rule
import org.junit.rules.TestName

import info.shelfunit.mail.ConfigHolder
import info.shelfunit.mail.meta.MetaProgrammer
import static info.shelfunit.mail.GETestUtils.addUser
import static info.shelfunit.mail.GETestUtils.getBase64Hash
import static info.shelfunit.mail.GETestUtils.getRandomString

import groovy.util.logging.Slf4j 

@Slf4j
class AUTHCommandSpec extends Specification {
    
    def crlf = "\r\n"
    static sqlObject
    static authCommand
    static rString    = getRandomString()
    static georgeW    = 'gw' + rString
    static johnA      = 'ja' + rString
    static jackO      = 'jo' + rString
    static domainList = [ 'shelfunit2.info', 'groovy-is-groovy2.org' ]
    static hamilton   = 'alexander2@shelfunit2.info'
    
    @Rule 
    TestName name = new TestName()
    
    def setup() {
        println "\n--- Starting test ${name.methodName}"
    }          // run before every feature method
    def cleanup() {}        // run after every feature method
    
    def setupSpec() {
        MetaProgrammer.runMetaProgramming()
        ConfigHolder.instance.setConfObject( "src/test/resources/application.test.conf" )
        sqlObject = ConfigHolder.instance.getSqlObject() 
        this.addUsers()
        authCommand = new AUTHCommand( sqlObject, domainList )
    }     // run before the first feature method
    
    def cleanupSpec() {
        sqlObject.execute "DELETE FROM email_user where username in ( ?, ?, ? )", [ georgeW, johnA, jackO ]
        // sqlObject.execute "DELETE FROM mail_spool_in where from_address = ?", [ ( jackO + '@stargate.mil' ) ]
        sqlObject.close()
    }   // run after the last feature method
   
    def addUsers() {
        addUser( sqlObject, 'George', 'Washington', georgeW, 'somePassword', true )
        addUser( sqlObject, 'John',   'Adams',      johnA,   'somePassword' )
        addUser( sqlObject, 'Jack',   "O'Neill",    jackO,   'somePassword' )
    }
    
	def "test handling with AUTH in previous command set"() {
        def prevCommandSetArg = [ 'AAAA', 'AUTH', 'BBBB', 'CCCC' ] as Set
	    when:
	        def resultMap    = authCommand.process( "The next meeting of the board of directors will be on Tuesday.\nJohn.", prevCommandSetArg, [ : ] )
	        def mailResponse = resultMap.resultString + crlf 
	    then:
	        mailResponse             == "503 Bad sequence of commands\r\n"
	        resultMap.prevCommandSet == prevCommandSetArg
	}

	def "test handling with AUTH only in command"() {
        def prevCommandSetArg = [ 'AAAA', 'BBBB', 'CCCC' ] as Set
	    when:
	        def resultMap    = authCommand.process( "AUTH", prevCommandSetArg, [ : ] )
	        def mailResponse = resultMap.resultString + crlf 
	    then:
	        mailResponse             == "501 Command not in proper form\r\n"
	        resultMap.prevCommandSet == prevCommandSetArg
	}
	
	def "test handling with AUTH and something after it besides PLAIN"() {
        def prevCommandSetArg = [ 'AAAA', 'BBBB', 'CCCC' ] as Set
	    when:
	        def resultMap    = authCommand.process( "AUTH PLANE", prevCommandSetArg, [ : ] )
	        def mailResponse = resultMap.resultString + crlf 
	    then:
	        mailResponse             == "501 Command not in proper form\r\n"
	        resultMap.prevCommandSet == prevCommandSetArg
	}
	
	def "test handling with AUTH PLAIN with no string afterward"() {
        def prevCommandSetArg = [ 'AAAA', 'BBBB', 'CCCC' ] as Set
	    when:
	        def resultMap    = authCommand.process( "AUTH PLAIN", prevCommandSetArg, [ : ] )
	        def mailResponse = resultMap.resultString + crlf 
	    then:
	        mailResponse             == "501 Command not in proper form\r\n"
	        resultMap.prevCommandSet == prevCommandSetArg
	}
	
		def "test handling with AUTH PLAIN and with bad string afterward"() {
        def prevCommandSetArg = [ 'AAAA', 'BBBB', 'CCCC' ] as Set
        def hashString = getBase64Hash( georgeW, 'somePasswore' )
	    when:
	        def resultMap    = authCommand.process( "AUTH PLAIN ${hashString}", prevCommandSetArg, [ : ] )
	        def mailResponse = resultMap.resultString + crlf 
	    then:
	        mailResponse             == "535 5.7.8  Authentication credentials invalid\r\n"
	        resultMap.prevCommandSet == prevCommandSetArg
	}
	
	def "test handling with AUTH PLAIN and with good string afterward"() {
        def prevCommandSetArg = [ 'AAAA', 'BBBB', 'CCCC' ] as Set
        def hashString = getBase64Hash( georgeW, 'somePassword' )
	    when:
	        def resultMap    = authCommand.process( "AUTH PLAIN ${hashString}", prevCommandSetArg, [ : ] )
	        def mailResponse = resultMap.resultString + crlf 
	    then:
	        mailResponse == "235 2.7.0 Authentication successful\r\n"
	        resultMap.prevCommandSet == prevCommandSetArg
	        resultMap.bufferMap.userInfo.logged_in == true
	}
	

	
} // line 150

