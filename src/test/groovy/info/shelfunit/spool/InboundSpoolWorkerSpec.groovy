package info.shelfunit.spool

import spock.lang.Ignore
import spock.lang.Requires
import spock.lang.Specification
import spock.lang.Stepwise

import java.io.InputStream
// import java.io.OutputStream

import org.junit.Rule
import org.junit.rules.TestName

import info.shelfunit.mail.ConfigHolder
import static info.shelfunit.mail.GETestUtils.addUser
import static info.shelfunit.mail.GETestUtils.getRandomString
import static info.shelfunit.mail.GETestUtils.getTableCount

import info.shelfunit.mail.meta.MetaProgrammer
// import info.shelfunit.smtp.command.EHLOCommand

import fi.solita.clamav.ClamAVClient

@Stepwise
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
    static config
    static realClamAVClient
    static uuidList = []
    static fromString
    static params = []
    static sqlCountString = 'select count(*) from mail_spool_in where status_string = ? and from_address = ?'
    static sqlCountStoreString = 'select count(*) from mail_store where from_address = ?'
    
    
    def setup() {
        println "\n--- Starting test ${name.methodName}"
    }          // run before every feature method
    
    def cleanup() {}        // run after every feature method
    
    def setupSpec() {
        MetaProgrammer.runMetaProgramming()
        ConfigHolder.instance.setConfObject( "src/test/resources/application.test.conf" )
        sql = ConfigHolder.instance.getSqlObject() 
        this.addUsers()
        config = ConfigHolder.instance.getConfObject()
        def host = config.clamav.hostname
        def port = config.clamav.port
        realClamAVClient = this.createClamAVClient()
        fromString = getRandomString() + "@" + getRandomString() + ".com"
        isw = new InboundSpoolWorker()
        
    }     // run before the first feature method
    
    def cleanupSpec() {
        sql.execute "DELETE FROM email_user where username in ( ?, ?, ? )", [ gwString, jaString, tjString ]
        sql.execute "DELETE FROM mail_spool_in where from_address = ?", [ fromString ]
        sql.close()
    }   // run after the last feature method
    
    def addUsers() {
        addUser( sql, 'George', 'Washington', gwString, 'somePassword' )
        addUser( sql, 'John', 'Adams', jaString, 'somePassword' )
        addUser( sql, 'Jack', "O'Neill", tjString, 'somePassword' )
    }
    
    def createClamAVClient() {
        def host = config.clamav.hostname
        def port = config.clamav.port
        println "About to return new client"
        return new ClamAVClient( host, port.toInt() )
    }
    
    def insertIntoMailSpoolIn( status, toAddress = gwString + '@' + domainList[ 0 ]  ) {
        params.clear()
        def uuid = UUID.randomUUID()
        params << uuid // id
        params << fromString // from_address
        params << " "        // from_username
        params << " "        // from_domain,
        params << toAddress  // to_address_list,
        params << getRandomString( 500 ) // text_body,
        params << status // status_string,
        params << "" //  base_64_hash
        
        sql.execute 'insert into mail_spool_in( id, from_address, from_username, from_domain, to_address_list,  text_body, status_string, base_64_hash ) values (?, ?, ?, ?, ?, ?, ?, ?)', params
        println "Entered ${uuid} with ${fromString}"
    }
    
    // in the closure for Requires, you can use "properties" instead of "System.properties"
    // -Dclam.live.daemon=true
    @Requires({ properties[ 'clam.live.daemon' ] == 'true' })
	def "test with actual clam client running - default to ignore"() {
	    when:
	        5.times { insertIntoMailSpoolIn( 'ENTERED' ) }
	        insertIntoMailSpoolIn( 'ENTERED', gwString + '@' + domainList[ 0 ] + ',' + jaString + '@' + domainList[0 ] )
	        insertIntoMailSpoolIn( 'ENTERED', gwString + '@' + domainList[ 0 ] + ',' +  getRandomString() + '@' + domainList[0 ] )
	        def enteredCount = getTableCount( sql, sqlCountString, [ 'ENTERED', fromString ] )
	        def cleanCount   = getTableCount( sql, sqlCountString, [ 'CLEAN', fromString ] )
	    then:
	        enteredCount == 7
	        cleanCount == 0

	    when:
	        isw.runClam( sql, realClamAVClient )
	        enteredCount = getTableCount( sql, sqlCountString, [ 'ENTERED', fromString ] )
	        cleanCount = getTableCount( sql, sqlCountString, [ 'CLEAN', fromString ] )
	    then:
	        1 == 1
	        enteredCount == 0
	        cleanCount == 7
	}
	
	@Requires({ properties[ 'clam.live.daemon' ] != 'true' })
	def "test cleaning messages with mocks"() {
	    println "\n--- Starting test ${name.methodName}"
	    def clamavMock = Mock( ClamAVClient )
	    def inputStreamMock = Mock( InputStream )
	    byte[] outputMock = "OK".getBytes()
	    def numTimes = 5
	    when:
	        numTimes.times { insertIntoMailSpoolIn( 'ENTERED' ) }
	        insertIntoMailSpoolIn( 'ENTERED', gwString + '@' + domainList[ 0 ] + ',' + jaString + '@' + domainList[ 0 ] )
	        insertIntoMailSpoolIn( 'ENTERED', gwString + '@' + domainList[ 0 ] + ',' +  getRandomString() + '@' + domainList[ 0 ] )
	        numTimes += 2
	        def enteredCount = getTableCount( sql, sqlCountString, [ 'ENTERED', fromString ] )
	        def cleanCount = getTableCount( sql, sqlCountString, [ 'CLEAN', fromString ] )
	    then:
	        enteredCount == numTimes
	        cleanCount == 0
	    when:
	        clamavMock.scan( _ ) >> outputMock
	        outputMock.toString() >> "Hello"
	        isw.runClam( sql, clamavMock )
	        enteredCount = getTableCount( sql, sqlCountString, [ 'ENTERED', fromString ] )
	        cleanCount = getTableCount( sql, sqlCountString, [ 'CLEAN', fromString ] )
	    then:
	        _ * ClamAVClient.isCleanReply( outputMock ) // subscriber.receive("hello")
	        1 == 1
	        enteredCount == 0
	        cleanCount == numTimes
	}
	
	@Requires({ properties[ 'clam.live.daemon' ] != 'true' })
	def "test unclean messages with mocks"() {
	    println "\n--- Starting test ${name.methodName}"
	    def clamavMock = Mock( ClamAVClient )
	    def inputStreamMock = Mock( InputStream )
	    byte[] outputMock = "FOUND".getBytes()
	    def numTimes = 6
	    when:
	        numTimes.times { insertIntoMailSpoolIn( 'ENTERED' ) }
	        def enteredCount = getTableCount( sql, sqlCountString, [ 'ENTERED', fromString ] )
	        def uncleanCount = getTableCount( sql, sqlCountString, [ 'UNCLEAN', fromString ] )
	    then:
	        enteredCount == numTimes
	        uncleanCount == 0
	        
	    when:
	        clamavMock.scan( _ ) >> outputMock
	        outputMock.toString() >> "Hello"
	        isw.runClam( sql, clamavMock )
	        enteredCount = getTableCount( sql, sqlCountString, [ 'ENTERED', fromString ] )
	        uncleanCount = getTableCount( sql, sqlCountString, [ 'UNCLEAN', fromString ] )
	    then:
	        _ * ClamAVClient.isCleanReply( outputMock ) 
	        1 == 1
	        enteredCount == 0
	        uncleanCount == numTimes
	}
	
	def "test transferring clean messages"() {
	    when:
	        def transferredCount = getTableCount( sql, sqlCountString, [ 'TRANSFERRED', fromString ] )
	        def cleanCount = getTableCount( sql, sqlCountString, [ 'CLEAN', fromString ] )
	        def storeCount = getTableCount( sql, sqlCountStoreString, [ fromString ] )
	    then:
	        transferredCount == 0
	        cleanCount == 7
	        storeCount == 0

	    when:
	        isw.moveCleanMessages( sql )
	        transferredCount = getTableCount( sql, sqlCountString, [ 'TRANSFERRED', fromString ] )
	        cleanCount = getTableCount( sql, sqlCountString, [ 'CLEAN', fromString ] )
	        storeCount = getTableCount( sql, sqlCountStoreString, [ fromString ] )
	    then:
	        1 == 1
	        transferredCount == 7
	        cleanCount == 0
	        storeCount == 8
	}
	
	def "test deleting transferred messages"() {
	    when:
	        def transferredCount = getTableCount( sql, sqlCountString, [ 'TRANSFERRED', fromString ] )
	        def storeCount = getTableCount( sql, sqlCountStoreString, [ fromString ] )
	    then:
	        transferredCount == 7
	        storeCount == 8

	    when:
	        isw.deleteTransferredMessages( sql )
	        transferredCount = getTableCount( sql, sqlCountString, [ 'TRANSFERRED', fromString ] )
	        storeCount = getTableCount( sql, sqlCountStoreString, [ fromString ] )
	    then:
	        1 == 1
	        transferredCount == 0
	        storeCount == 8
	}
	
	def "test deleting transferred messages if it is empty"() {
	    when:
	        def transferredCount = getTableCount( sql, sqlCountString, [ 'TRANSFERRED', fromString ] )
	        def storeCount = getTableCount( sql, sqlCountStoreString, [ fromString ] )
	    then:
	        transferredCount == 0
	        storeCount == 8

	    when:
	        isw.deleteTransferredMessages( sql )
	        transferredCount = getTableCount( sql, sqlCountString, [ 'TRANSFERRED', fromString ] )
	        storeCount = getTableCount( sql, sqlCountStoreString, [ fromString ] )
	    then:
	        1 == 1
	        transferredCount == 0
	        storeCount == 8
	}
	
	def "test delete unclean messages"() {
	    def numUnclean = 5
	    def uncleanMessages = 0
	    when:
	        numUnclean.times { insertIntoMailSpoolIn( 'UNCLEAN' ) }
	        uncleanMessages = getTableCount( sql, sqlCountString, [ 'UNCLEAN', fromString ] )
	        println "numUnclean == ${numUnclean} and uncleanMessages == ${uncleanMessages}"
	    then:
	        numUnclean != uncleanMessages
	    when:
	        isw.deleteUncleanMessages( sql )
	        uncleanMessages = getTableCount( sql, sqlCountString, [ 'UNCLEAN', fromString ] )
	    then:
	        uncleanMessages == 0
	        
	}

	@Ignore
	def "always ignore"() {
	    expect:
	        1 == 1
	}
	
} // line 246

