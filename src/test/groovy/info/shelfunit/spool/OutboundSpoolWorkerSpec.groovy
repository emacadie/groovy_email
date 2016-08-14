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
import static info.shelfunit.mail.GETestUtils.getBase64Hash
import static info.shelfunit.mail.GETestUtils.getRandomString
import static info.shelfunit.mail.GETestUtils.getTableCount

import info.shelfunit.mail.meta.MetaProgrammer
// import info.shelfunit.smtp.command.EHLOCommand

import fi.solita.clamav.ClamAVClient

@Stepwise
class OutboundSpoolWorkerSpec extends Specification {
    @Rule 
    TestName name = new TestName()
    
    def crlf = "\r\n"
    static sql
    static domainList = [ 'shelfunit.info', 'groovy-is-groovy.org' ]
    static rString    = getRandomString()
    static gwString   = 'gw' + rString
    static jaString   = 'ja' + rString
    static tjString   = 'tj' + rString
    static fromString = gwString + '@' + domainList[ 0 ]
    static gwBase64Hash = getBase64Hash( gwString, 'somePassword' )
    static OutboundSpoolWorker osw
    static config
    static realClamAVClient
    static uuidList = []
    static params = []
    static sqlCountString = 'select count(*) from mail_spool_out where status_string = ? and from_address = ?'
    static sqlCountStoreString = 'select count(*) from mail_store where from_address = ?'
    
    
    def setup() {
        println "\n--- Starting test ${name.methodName}"
    } // run before every feature method
    
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
        
        osw = new OutboundSpoolWorker()
        
        this.enterOutgoingMessages()
        
    }     // run before the first feature method
    
    def cleanupSpec() {
        sql.execute "DELETE FROM email_user where username like ?", [ '%' + rString ]
        sql.execute "DELETE FROM mail_spool_out where from_address = ?", [ gwString + '@' + domainList[ 0 ] ]
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
    
    def insertIntoMailSpoolOut( status, toAddress, message = getRandomString( 500 ), uuid = UUID.randomUUID() ) {
        params.clear()
        params << uuid
        params << gwString + '@' + domainList[ 0 ]
        params << toAddress
        params << message
        params << 'ENTERED'
        params << gwBase64Hash
        sql.execute 'insert into mail_spool_out( id, from_address, to_address_list, text_body, status_string, base_64_hash ) values (?, ?, ?, ?, ?, ?)', params
    }
    
    def enterOutgoingMessages() {
        insertIntoMailSpoolOut( 'ENTERED', jaString + '@' + domainList[ 0 ] )
        insertIntoMailSpoolOut( 'ENTERED', 'oneill@stargate.mil' )
        insertIntoMailSpoolOut( 'ENTERED', 'smtp@averagesmtp.com,oneill@stargate.mil' )
        insertIntoMailSpoolOut( 'ENTERED', jaString + '@' + domainList[ 0 ] + ',jack9@gmail.com,jack@yahoo.com' )
        insertIntoMailSpoolOut( 'ENTERED', 'oneill@stargate.mil,scarter@stargate.mil,weir@atlantis.mil,mckay@atlantis.mil' )
        insertIntoMailSpoolOut( 'ENTERED', 'weir@atlantis.mil,weir@replicators.org' )
        insertIntoMailSpoolOut( 'ENTERED', 'rush@destiny.ancients.com,young@destiny.ancients.com' )
    }
    
    // in the closure for Requires, you can use "properties" instead of "System.properties"
    // -Dclam.live.daemon=true

    @Requires({ properties[ 'clam.live.daemon' ] == 'true' })
	def "test with actual clam client running - default to ignore"() {
	    when:
	        
	        def enteredCount = getTableCount( sql, sqlCountString, [ 'ENTERED', fromString ] )
	        def cleanCount = getTableCount( sql, sqlCountString, [ 'CLEAN', fromString ] )
	    then:
	        enteredCount == 7
	        cleanCount == 0

	    when:
	        osw.runClam( sql, realClamAVClient )
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
	    def numTimes = 7
	    when:
	        def enteredCount = getTableCount( sql, sqlCountString, [ 'ENTERED', fromString ] )
	        def cleanCount = getTableCount( sql, sqlCountString, [ 'CLEAN', fromString ] )
	    then:
	        enteredCount == numTimes
	        cleanCount == 0
	    when:
	        clamavMock.scan( _ ) >> outputMock
	        outputMock.toString() >> "Hello"
	        osw.runClam( sql, clamavMock )
	        enteredCount = getTableCount( sql, sqlCountString, [ 'ENTERED', fromString ] )
	        cleanCount = getTableCount( sql, sqlCountString, [ 'CLEAN', fromString ] )
	    then:
	        _ * ClamAVClient.isCleanReply( outputMock ) // subscriber.receive("hello")
	        1 == 1
	        enteredCount == 0
	        cleanCount == numTimes
	}
	
	@Ignore
	def "test deliverMessages( sql, domainList, outgoingPort )"() {
	    println "\n--- Starting test ${name.methodName}"
	    def mockSender = Mock( MessageSender )
	}
	
	@Requires({ properties[ 'clam.live.daemon' ] != 'true' })
	def "test unclean messages with mocks"() {
	    println "\n--- Starting test ${name.methodName}"
	    def clamavMock = Mock( ClamAVClient )
	    def inputStreamMock = Mock( InputStream )
	    byte[] outputMock = "FOUND".getBytes()
	    def numTimes = 6
	    when:
	        numTimes.times { insertIntoMailSpoolOut( 'ENTERED', 'weir@atlantis.mil,weir@replicators.org' ) }
	        def enteredCount = getTableCount( sql, sqlCountString, [ 'ENTERED', fromString ] )
	        def uncleanCount = getTableCount( sql, sqlCountString, [ 'UNCLEAN', fromString ] )
	    then:
	        enteredCount == numTimes
	        uncleanCount == 0
	    when:
	        clamavMock.scan( _ ) >> outputMock
	        outputMock.toString() >> "Hello"
	        osw.runClam( sql, clamavMock )
	        enteredCount = getTableCount( sql, sqlCountString, [ 'ENTERED', fromString ] )
	        uncleanCount = getTableCount( sql, sqlCountString, [ 'UNCLEAN', fromString ] )
	    then:
	        _ * ClamAVClient.isCleanReply( outputMock ) 
	        1 == 1
	        enteredCount == 0
	        uncleanCount == numTimes
	}
	
	def "test delete unclean messages"() {
	    def numUnclean = 5
	    def uncleanMessages = 0
	    when:
	        numUnclean.times { 
	            insertIntoMailSpoolOut( 'UNCLEAN', "${getRandomString( 10 )}@${getRandomString( 10 )}.com".toString() ) 
	        }
	        uncleanMessages = getTableCount( sql, sqlCountString, [ 'UNCLEAN', fromString ] )
	        println "numUnclean == ${numUnclean} and uncleanMessages == ${uncleanMessages}"
	    then:
	        numUnclean != uncleanMessages
	    when:
	        osw.deleteUncleanMessages( sql )
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

