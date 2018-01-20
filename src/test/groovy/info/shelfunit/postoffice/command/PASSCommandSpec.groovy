package info.shelfunit.postoffice.command

import spock.lang.Specification
// import spock.lang.Unroll

import org.junit.Rule
import org.junit.rules.TestName

import info.shelfunit.mail.meta.MetaProgrammer
import info.shelfunit.mail.ConfigHolder

import static info.shelfunit.mail.GETestUtils.addUser
import static info.shelfunit.mail.GETestUtils.getRandomString
import static info.shelfunit.mail.GETestUtils.getUserInfo

import groovy.util.logging.Slf4j 

import org.apache.shiro.crypto.hash.Sha512Hash

@Slf4j
class PASSCommandSpec extends Specification {
    
    def crlf = "\r\n"
    static domainList   = [ 'shelfunit.info', 'groovy-is-groovy.org' ]
    static iterations   = 10000
    static passCommand
    static somePassword = 'somePassword'
    static sqlObject
    static rString      = getRandomString( 12 )
    static gwPASS       = 'gw' + rString // @shelfunit.info'
    static jaPASS       = 'ja' + rString // @shelfunit.info'
    static onPASS       = 'on' + rString // @shelfunit.info'
    static moPASS       = 'mo' + rString // @shelfunit.info'

    @Rule 
    TestName name = new TestName()
    
    def setup() {
        println "\n--- Starting test ${name.methodName}"
    }          // run before every feature method
    def cleanup() {}        // run after every feature method
    
    def setupSpec() {
        MetaProgrammer.runMetaProgramming()
        ConfigHolder.instance.setConfObject( "src/test/resources/application.test.conf" )
        def conf    = ConfigHolder.instance.getConfObject()
        sqlObject   = ConfigHolder.instance.getSqlObject()
        passCommand = new PASSCommand( sqlObject )
        this.addUsers()
    }     // run before the first feature method
    
    def cleanupSpec() {
        sqlObject.execute "DELETE FROM email_user where username in ( ?, ?, ?, ? )", [ gwPASS, jaPASS, onPASS, moPASS ]
        sqlObject.close()
    }   // run after the last feature method
    
    def addUsers() {
        addUser( sqlObject, 'George', 'Washington', gwPASS, somePassword )
        addUser( sqlObject, 'John',   'Adams',      jaPASS, somePassword )
        addUser( sqlObject, 'Jack',   "O'Neill",    onPASS, somePassword )
        addUser( sqlObject, 'More',   'SomeDude',   moPASS, somePassword )
    }
    
    def "test wrong buffer state"() {
	    def userInfo           = [:]
	    userInfo.username      = "some.user"
	    def password           = "this.is.a.password"
	    userInfo.password_algo = 'SHA-512'
	    userInfo.iterations    = iterations
	    userInfo.password_hash = new Sha512Hash( password, userInfo.username, iterations ) .toBase64()
	    userInfo.first_name    = 'some'
	    userInfo.last_name     = 'user'
	    userInfo.userid        = 10
	    
	    def bufferMap      = [:]
	    bufferMap.state    = 'TRANSACTION'
	    bufferMap.userInfo = userInfo
        def resultMap

	    when:
	        resultMap = passCommand.process( "PASS ${password}", [] as Set, bufferMap )
	    then:
	        resultMap.resultString    == "-ERR Not in AUTHORIZATION state"
	        resultMap.bufferMap.state == 'TRANSACTION'
	        
	    when:
	        bufferMap.state = 'AUTHORIZATION'
	        resultMap = passCommand.process( "PASS tdsgghrd", [] as Set, bufferMap )
	    then:
	        resultMap.resultString == "-ERR ${userInfo.username} not authenticated"
	        resultMap.bufferMap.state == 'AUTHORIZATION'
	}

	def "the first password attempt"() {
	    def userInfo           = [:]
	    userInfo.username      = "some.user"
	    def password           = "this.is.a.password"
	    userInfo.iterations    = iterations
	    userInfo.password_algo = 'SHA-512'
	    userInfo.password_hash = new Sha512Hash( password, userInfo.username, iterations ) .toBase64()
	    userInfo.first_name    = 'some'
	    userInfo.last_name     = 'user'
	    userInfo.userid        = 10
	    
	    def bufferMap          = [:]
	    bufferMap.state        = 'AUTHORIZATION'
	    bufferMap.userInfo     = userInfo
        def resultMap

	    when:
	        resultMap = passCommand.process( "PASS ${password}", [] as Set, bufferMap )
	    then:
	        resultMap.resultString    == "+OK ${userInfo.username} authenticated"
	        resultMap.bufferMap.state == 'TRANSACTION'
	        resultMap.bufferMap.timestamp.class.name == 'java.sql.Timestamp'
	        
	    when:
	        bufferMap.state = 'AUTHORIZATION'
	        resultMap = passCommand.process( "PASS tdsgghrd", [] as Set, bufferMap )
	    then:
	        resultMap.resultString    == "-ERR ${userInfo.username} not authenticated"
	        resultMap.bufferMap.state == 'AUTHORIZATION'
	        resultMap.bufferMap.timestamp == null
	}
	
	def "bad password does not change state or set timestamp"() {
	    def userInfo           = [:]
	    userInfo.username      = "some.user"
	    def password           = "this.is.a.password"
	    userInfo.password_algo = 'SHA-512'
	    userInfo.iterations    = iterations
	    userInfo.password_hash = new Sha512Hash( password, userInfo.username, iterations ) .toBase64()
	    userInfo.first_name    = 'some'
	    userInfo.last_name     = 'user'
	    userInfo.userid        = 10
	    
	    def bufferMap          = [:]
	    bufferMap.state        = 'AUTHORIZATION'
	    bufferMap.userInfo     = userInfo
        def resultMap
	   
	    when:
	        bufferMap.state = 'AUTHORIZATION'
	        resultMap       = passCommand.process( "PASS tdsgghrd", [] as Set, bufferMap )
	    then:
	        resultMap.resultString        == "-ERR ${userInfo.username} not authenticated"
	        resultMap.bufferMap.state     == 'AUTHORIZATION'
	        resultMap.bufferMap.timestamp == null
	}
	
	def "test created user with correct password"() {
	    def userInfo       = [:]
	    userInfo.username  = jaPASS
	    def password       = somePassword
	    	    
	    def bufferMap      = [:]
	    bufferMap.state    = 'AUTHORIZATION'
	    bufferMap.userInfo = getUserInfo( sqlObject, jaPASS )
        def resultMap

        when:
            bufferMap.userInfo = getUserInfo( sqlObject, jaPASS )
        then:
            bufferMap.userInfo.logged_in == false
        
	    when:
	        resultMap = passCommand.process( "PASS ${somePassword}", [] as Set, bufferMap )
	    then:
	        resultMap.resultString    == "+OK ${userInfo.username} authenticated"
	        resultMap.bufferMap.state == 'TRANSACTION'
	        resultMap.bufferMap.timestamp.class.name == 'java.sql.Timestamp'
	        
	    when:
            bufferMap.userInfo = getUserInfo( sqlObject, jaPASS )
        then:
            bufferMap.userInfo.logged_in == true
	}
	
	def "test created user with incorrect and correct password"() {
	    def userInfo       = [:]
	    userInfo.username  = onPASS
	    def password       = somePassword + 'p'
	    	    
	    def bufferMap      = [:]
	    bufferMap.state    = 'AUTHORIZATION'
	    bufferMap.userInfo = getUserInfo( sqlObject, onPASS )
        def resultMap

        when:
            bufferMap.userInfo = getUserInfo( sqlObject, onPASS )
        then:
            bufferMap.userInfo.logged_in == false
        
        when:
            resultMap = passCommand.process( "PASS ${password}", [] as Set, bufferMap )
        then:
            resultMap.resultString == "-ERR ${onPASS} not authenticated"
        when:
            bufferMap.userInfo = getUserInfo( sqlObject, onPASS )
        then:
            bufferMap.userInfo.logged_in == false
            
	    when:
	        resultMap = passCommand.process( "PASS ${somePassword}", [] as Set, bufferMap )
	    then:
	        resultMap.resultString    == "+OK ${userInfo.username} authenticated"
	        resultMap.bufferMap.state == 'TRANSACTION'
	        resultMap.bufferMap.timestamp.class.name == 'java.sql.Timestamp'
	        
	    when:
            bufferMap.userInfo = getUserInfo( sqlObject, onPASS )
        then:
            bufferMap.userInfo.logged_in == true
	}

    def "test created user with incorrect and correct password and case-insensitive commands"() {
	    def userInfo       = [:]
	    userInfo.username  = moPASS
	    def password       = somePassword + 'p'
	    	    
	    def bufferMap      = [:]
	    bufferMap.state    = 'AUTHORIZATION'
	    bufferMap.userInfo = getUserInfo( sqlObject, moPASS )
        def resultMap

        when:
            bufferMap.userInfo = getUserInfo( sqlObject, moPASS )
        then:
            bufferMap.userInfo.logged_in == false
        
        when:
            resultMap = passCommand.process( "PAss ${password}", [] as Set, bufferMap )
        then:
            resultMap.resultString == "-ERR ${onPASS} not authenticated"
        when:
            bufferMap.userInfo = getUserInfo( sqlObject, moPASS )
        then:
            bufferMap.userInfo.logged_in == false
            
	    when:
	        resultMap = passCommand.process( "pAsS ${somePassword}", [] as Set, bufferMap )
	    then:
	        resultMap.resultString    == "+OK ${userInfo.username} authenticated"
	        resultMap.bufferMap.state == 'TRANSACTION'
	        resultMap.bufferMap.timestamp.class.name == 'java.sql.Timestamp'
	        
	    when:
            bufferMap.userInfo = getUserInfo( sqlObject, moPASS )
        then:
            bufferMap.userInfo.logged_in == true
	}
	

} // line 172

