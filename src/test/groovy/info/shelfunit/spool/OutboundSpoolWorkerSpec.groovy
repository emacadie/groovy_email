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
    static sqlObject
    static domainList   = [ 'shelfunit.info', 'groovy-is-groovy.com' ]
    static rString      = getRandomString()
    static gwString     = 'gw' + rString
    static jaString     = 'ja' + rString
    static tjString     = 'tj' + rString
    static fromString   = gwString + '@' + domainList[ 0 ]
    static gwBase64Hash = getBase64Hash( gwString, 'somePassword' )
    static uuidList     = []
    static params       = []
    static config
    static realClamAVClient
    static OutboundSpoolWorker osw  = new OutboundSpoolWorker()
    static sqlCountString           = 'select count(*) from mail_spool_out where status_string = ? and from_address = ?'
    static sqlCountStoreString      = 'select count(*) from mail_store where from_address = ?'
    
    
    def setup() {
        println "\n--- Starting test ${name.methodName}"
    } // run before every feature method
    
    def cleanup() {}        // run after every feature method
    
    def setupSpec() {
        MetaProgrammer.runMetaProgramming()
        ConfigHolder.instance.setConfObject( "src/test/resources/application.test.conf" )
        sqlObject = ConfigHolder.instance.getSqlObject() 
        config    = ConfigHolder.instance.getConfObject()
        def host  = config.clamav.hostname
        def port  = config.clamav.port
        realClamAVClient = this.createClamAVClient()
        this.addUsers()
        // osw = new OutboundSpoolWorker()
        
        this.enterOutgoingMessages()
        
    }     // run before the first feature method
    
    def cleanupSpec() {
        sqlObject.execute "DELETE FROM email_user where username like ?", [ '%' + rString ]
        sqlObject.execute "DELETE FROM mail_spool_out where from_username like ?", [ '%' + rString ] 
        sqlObject.close()
    }   // run after the last feature method
    
    def addUsers() {
        addUser( sqlObject, 'George', 'Washington', gwString, 'somePassword' )
        addUser( sqlObject, 'John',   'Adams',      jaString, 'somePassword' )
        addUser( sqlObject, 'Jack',   "O'Neill",    tjString, 'somePassword' )
    }
    
    def createClamAVClient() {
        def host = config.clamav.hostname
        def port = config.clamav.port
        println "About to return new client"
        return new ClamAVClient( host, port.toInt() )
    }
    
    def insertIntoMailSpoolOut( status, toAddress, userName = gwString, message = getRandomString( 500 ), uuid = UUID.randomUUID(), userDomain = domainList[ 0 ] ) {
        params.clear() 
        params << uuid // id,
        params << userName + '@' + domainList[ 0 ] // from_address
        params << userName // from_username, 
        params << userDomain // from_domain
        params << toAddress  // to_address_list
        params << message    // text_body
        params << status     // status_string
        params << gwBase64Hash // base_64_hash
        sqlObject.execute 'insert into mail_spool_out( id, from_address, from_username, from_domain, to_address_list, text_body, status_string, base_64_hash ) values (?, ?, ?, ?, ?, ?, ?, ?)', params
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
	        def enteredCount = getTableCount( sqlObject, sqlCountString, [ 'ENTERED', fromString ] )
	        def cleanCount   = getTableCount( sqlObject, sqlCountString, [ 'CLEAN', fromString ] )
	    then:
	        enteredCount == 7
	        cleanCount   == 0

	    when:
	        osw.runClam( sqlObject, realClamAVClient )
	        enteredCount = getTableCount( sqlObject, sqlCountString, [ 'ENTERED', fromString ] )
	        cleanCount   = getTableCount( sqlObject, sqlCountString, [ 'CLEAN', fromString ] )
	    then:
	        1 == 1
	        enteredCount == 0
	        cleanCount   == 7
	}
	
	@Requires({ properties[ 'clam.live.daemon' ] != 'true' })
	def "test cleaning messages with mocks"() {
	    println "\n--- Starting test ${name.methodName}"
	    def clamavMock       = Mock( ClamAVClient )
	    def inputStreamMock  = Mock( InputStream )
	    byte[] outputMock    = "OK".getBytes()
	    def numTimes = 7
	    when:
	        def enteredCount = getTableCount( sqlObject, sqlCountString, [ 'ENTERED', fromString ] )
	        def cleanCount   = getTableCount( sqlObject, sqlCountString, [ 'CLEAN', fromString ] )
	        println "enteredCount: ${enteredCount}, cleanCount: ${cleanCount}"
	    then:
	        enteredCount == numTimes
	        cleanCount   == 0
	    when:
	        clamavMock.scan( _ ) >> outputMock
	        outputMock.toString() >> "Hello"
	        osw.runClam( sqlObject, clamavMock )
	        enteredCount = getTableCount( sqlObject, sqlCountString, [ 'ENTERED', fromString ] )
	        cleanCount   = getTableCount( sqlObject, sqlCountString, [ 'CLEAN', fromString ] )
	        println "enteredCount: ${enteredCount}, cleanCount: ${cleanCount}"
	    then:
	        _ * ClamAVClient.isCleanReply( outputMock ) // subscriber.receive("hello")
	        1 == 1
	        enteredCount == 0
	        cleanCount   == numTimes
	}
	
	@Ignore
	def "test deliverMessages( sqlObject, domainList, outgoingPort )"() {
	    println "\n--- Starting test ${name.methodName}"
	    def mockSender = Mock( MessageSender )
	}
	
	@Requires({ properties[ 'clam.live.daemon' ] != 'true' })
	def "test unclean messages with mocks"() {
	    println "\n--- Starting test ${name.methodName}"
	    def clamavMock      = Mock( ClamAVClient )
	    def inputStreamMock = Mock( InputStream )
	    byte[] outputMock   = "FOUND".getBytes()
	    def numTimes        = 6
	    when:
	        numTimes.times { insertIntoMailSpoolOut( 'ENTERED', 'weir@atlantis.mil,weir@replicators.org' ) }
	        def enteredCount = getTableCount( sqlObject, sqlCountString, [ 'ENTERED', fromString ] )
	        def uncleanCount = getTableCount( sqlObject, sqlCountString, [ 'UNCLEAN', fromString ] )
	        println "enteredCount: ${enteredCount}, uncleanCount: ${uncleanCount}"
	    then:
	        enteredCount == numTimes
	        uncleanCount == 0
	    when:
	        clamavMock.scan( _ ) >> outputMock
	        outputMock.toString() >> "Hello"
	        osw.runClam( sqlObject, clamavMock )
	        enteredCount = getTableCount( sqlObject, sqlCountString, [ 'ENTERED', fromString ] )
	        uncleanCount = getTableCount( sqlObject, sqlCountString, [ 'UNCLEAN', fromString ] )
	        println "enteredCount: ${enteredCount}, uncleanCount: ${uncleanCount}"
	    then:
	        _ * ClamAVClient.isCleanReply( outputMock ) 
	        1 == 1
	        enteredCount == 0
	        uncleanCount == numTimes
	}
	
	def "test delete unclean messages"() {
	    def numUnclean      = 5
	    def uncleanMessages = 0
	    when:
	        numUnclean.times { 
	            insertIntoMailSpoolOut( 'UNCLEAN', "${getRandomString( 10 )}@${getRandomString( 10 )}.com".toString() ) 
	        }
	        uncleanMessages = getTableCount( sqlObject, sqlCountString, [ 'UNCLEAN', fromString ] )
	        println "numUnclean == ${numUnclean} and uncleanMessages == ${uncleanMessages}"
	    then:
	        numUnclean != uncleanMessages
	    when:
	        osw.deleteUncleanMessages( sqlObject )
	        uncleanMessages = getTableCount( sqlObject, sqlCountString, [ 'UNCLEAN', fromString ] )
	        println "numUnclean == ${numUnclean} and uncleanMessages == ${uncleanMessages}"
	    then:
	        uncleanMessages == 0
	        
	}
	
	def "test if outgoing messages from invalid users are deleted"() {
	    def cleanCountStart = getTableCount( sqlObject, 'select count(*) from mail_spool_out where status_string = ?', [ 'CLEAN' ] )
	    println "cleanCountStart: ${cleanCountStart}"
	    sqlObject.eachRow( 'select from_address from mail_spool_out where status_string = ?', [ 'CLEAN' ] ) { row ->
            println "here is user name of clean message: ${row[ 'from_address' ]}"
        } // sqlObject.eachRow
        when:
            def f = 0
        then:
            1 == 1
        def badUserCount = 5
        def badUserName
        badUserCount.times {
            badUserName = getRandomString( 10 ) 
            insertIntoMailSpoolOut( 
                'CLEAN', // "${getRandomString( 10 )}@${getRandomString( 10 )}.com".toString(), 
                'rrrr@rrrr.com',
                badUserName
            ) 
            println "Inserted outward spool with user name ${badUserName}"
        }
        when:
            def newCleanCount = getTableCount( 
                sqlObject, 'select count(*) from mail_spool_out where status_string = ?', [ 'CLEAN' ] 
            )
            println "newCleanCount: ${newCleanCount}"
        then:
            newCleanCount == badUserCount + cleanCountStart
        when:
            osw.findInvalidUsers( sqlObject, domainList ) 
            def invalidCount = getTableCount( 
                sqlObject, 'select count(*) from mail_spool_out where status_string = ?', [ 'INVALID_USER' ] 
            )
        then:
            invalidCount == badUserCount
        when:
            osw.deleteInvalidUserMessages( sqlObject )
            invalidCount = getTableCount( 
                sqlObject, 'select count(*) from mail_spool_out where status_string = ?', [ 'INVALID_USER' ] 
            )
        then:
            invalidCount == 0
        when:
            def cleanCountEnd = getTableCount( sqlObject, 'select count(*) from mail_spool_out where status_string = ?', [ 'CLEAN' ] )
        then:
            cleanCountStart == cleanCountEnd
	} // "test if outgoing messages from invalid users are deleted" 
	
	def "test if outgoing messages with invalid domains are deleted"() {
	    def cleanCountStart = getTableCount( sqlObject, 'select count(*) from mail_spool_out where status_string = ?', [ 'CLEAN' ] )
	    println "cleanCountStart: ${cleanCountStart}"
	    sqlObject.eachRow( 'select from_address from mail_spool_out where status_string = ?', [ 'CLEAN' ] ) { row ->
            println "here is user name of clean message: ${row[ 'from_address' ]}"
        } // sqlObject.eachRow
        when:
            def f = 0
        then:
            1 == 1
        def badDomainCount = 5
        def badDomainName
        badDomainCount.times {
            badDomainName = getRandomString( 10 ) + ".com"
            insertIntoMailSpoolOut( 
                'CLEAN',     // status, 
                'rr@rr.com', // toAddress, 
                gwString,    // userName = gwString, 
                getRandomString( 500 ), // message = getRandomString( 500 ), 
                UUID.randomUUID(), // uuid = UUID.randomUUID(), 
                badDomainName      // userDomain = domainList[ 0 ] 
            )
            println "Inserted outward spool with domain name ${badDomainName}"
        }
        when:
            def newCleanCount = getTableCount( 
                sqlObject, 'select count(*) from mail_spool_out where status_string = ?', [ 'CLEAN' ] 
            )
            println "newCleanCount: ${newCleanCount}"
        then:
            newCleanCount == badDomainCount + cleanCountStart
        when:
            osw.findInvalidDomains( sqlObject, domainList ) 
            def invalidCount = getTableCount( 
                sqlObject, 'select count(*) from mail_spool_out where status_string = ?', [ 'INVALID_DOMAIN' ] 
            )
        then:
            invalidCount == badDomainCount
        when:
            osw.deleteInvalidDomainMessages( sqlObject )
            invalidCount = getTableCount( 
                sqlObject, 'select count(*) from mail_spool_out where status_string = ?', [ 'INVALID_DOMAIN' ] 
            )
        then:
            invalidCount == 0
        when:
            def cleanCountEnd = getTableCount( sqlObject, 'select count(*) from mail_spool_out where status_string = ?', [ 'CLEAN' ] )
        then:
            cleanCountStart == cleanCountEnd
  
	} // "test if outgoing messages with invalid domains are deleted"

	def "test domain of different case"() {
	    def cleanCountStart = getTableCount( sqlObject, 'select count(*) from mail_spool_out where status_string = ?', [ 'CLEAN' ] )
        println "domainList[ 0 ]: ${domainList[ 0 ]}"
        println "domainList[ 0 ].toUpperCase(): ${domainList[ 0 ].toUpperCase()}"
	    println "cleanCountStart: ${cleanCountStart}"
	    sqlObject.eachRow( 'select from_address from mail_spool_out where status_string = ?', [ 'CLEAN' ] ) { row ->
            println "here is user name of clean message: ${row[ 'from_address' ]}"
        } // sqlObject.eachRow
        when:
            def f = 0
        then:
            1 == 1
        def badDomainCount = 5
        badDomainCount.times {
            insertIntoMailSpoolOut( 
                'CLEAN',     // status, 
                'rr@rr.com', // toAddress, 
                gwString,    // userName = gwString, 
                getRandomString( 500 ), // message = getRandomString( 500 ), 
                UUID.randomUUID(), // uuid = UUID.randomUUID(), 
                domainList[ 0 ].toUpperCase()     // userDomain = domainList[ 0 ] 
            )
            println "Inserted outward spool with domain name ${domainList[ 0 ].toUpperCase()}"
        }
        when:
            def newCleanCount = getTableCount( 
                sqlObject, 'select count(*) from mail_spool_out where status_string = ?', [ 'CLEAN' ] 
            )
            println "newCleanCount: ${newCleanCount}"
        then:
            newCleanCount == badDomainCount + cleanCountStart
        when:
            osw.findInvalidDomains( sqlObject, domainList ) 
            def invalidCount = getTableCount( 
                sqlObject, 'select count(*) from mail_spool_out where status_string = ?', [ 'INVALID_DOMAIN' ] 
            )
            println "invalidCount: ${invalidCount}"
        then:
            invalidCount == 0
        when:
            osw.deleteInvalidDomainMessages( sqlObject )
            invalidCount = getTableCount( 
                sqlObject, 'select count(*) from mail_spool_out where status_string = ?', [ 'INVALID_DOMAIN' ] 
            )
        then:
            invalidCount == 0
        when:
            def cleanCountEnd = getTableCount( sqlObject, 'select count(*) from mail_spool_out where status_string = ?', [ 'CLEAN' ] )
        then:
            ( badDomainCount ) + cleanCountStart == cleanCountEnd
  
	} // "test if outgoing messages with invalid domains are deleted"

	
	@Ignore
	def "always ignore"() {
	    expect:
	        1 == 1
	}
	
} // line 246

