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


import fi.solita.clamav.ClamAVClient

@Stepwise
class InboundSpoolWorkerSpec extends Specification {
    @Rule 
    TestName name = new TestName()
    
    def crlf = "\r\n"
    static sqlObject
    static domainList = [ 'shelfunit.info', 'groovy-is-groovy.org' ]
    static rString    = getRandomString()
    static gwString   = 'gw' + rString
    static jaString   = 'ja' + rString
    static tjString   = 'tj' + rString
    static uuidList   = []
    static params     = []
    static fromArray  = [] as List
    static InboundSpoolWorker isw
    static config
    static realClamAVClient
    static fromString
    static sqlCountString      = 'select count(*) from mail_spool_in where status_string = ? and from_address = ?'
    static sqlCountStoreString = 'select count(*) from mail_store where from_address = ?'
    
    
    def setup() {
        println "\n--- Starting test ${name.methodName}"
    }          // run before every feature method
    
    def cleanup() {}        // run after every feature method
    
    def setupSpec() {
        MetaProgrammer.runMetaProgramming()
        ConfigHolder.instance.setConfObject( "src/test/resources/application.test.conf" )
        sqlObject = ConfigHolder.instance.getSqlObject() 
        this.addUsers()
        config   = ConfigHolder.instance.getConfObject()
        def host = config.clamav.hostname
        def port = config.clamav.port
        realClamAVClient = this.createClamAVClient()
        fromString       = getRandomString() + "@" + getRandomString() + ".com"
        isw              = new InboundSpoolWorker()
        
    }     // run before the first feature method
    
    def cleanupSpec() {
        
        println "here is rString: ${rString}"
        def fromArrayQMarkString = fromArray.getQMarkString()

        sqlObject.execute "DELETE FROM email_user where username like ( ? )", [ "%${rString}%".toString() ]
        sqlObject.execute "DELETE FROM email_user where username_lc like ( ? )", [ "%${fromString}%".toString() ]
        sqlObject.execute "DELETE FROM mail_spool_in where from_address in ( ${fromArrayQMarkString} )",  fromArray 
        sqlObject.execute "DELETE FROM mail_store where from_address in (${ fromArrayQMarkString })",  fromArray 
        sqlObject.execute "DELETE FROM bad_mail_store where from_address in (${ fromArrayQMarkString })",  fromArray 

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
    
    def insertIntoMailSpoolIn( status, toAddress = gwString + '@' + domainList[ 0 ], fromAddress = fromString  ) {
        params.clear()
        def uuid = UUID.randomUUID()
        params << uuid // id
        params << fromAddress // from_address
        params << " "        // from_username
        params << " "        // from_domain,
        params << toAddress  // to_address_list,
        params << getRandomString( 500 ) // text_body,
        params << status // status_string,
        params << "" //  base_64_hash
        
        sqlObject.execute "insert into mail_spool_in( id, from_address, from_username, " +
            "from_domain, to_address_list, text_body, status_string, base_64_hash ) " +
            "values (?, ?, ?, ?, ?, ?, ?, ?)", 
            params
        println "Entered ${uuid} with ${fromString}"
    }
    
    // in the closure for Requires, you can use "properties" instead of "System.properties"
    // -Dclam.live.daemon=true
    @Requires({ properties[ 'clam.live.daemon' ] == 'true' })
	def "test with actual clam client running - default to ignore"() {
	    when:
	        5.times { insertIntoMailSpoolIn( 'ENTERED' ) }
	        insertIntoMailSpoolIn( 'ENTERED', gwString + '@' + domainList[ 0 ] + ',' + jaString + '@' + domainList[ 0 ] )
	        insertIntoMailSpoolIn( 'ENTERED', gwString + '@' + domainList[ 0 ] + ',' +  getRandomString() + '@' + domainList[ 0 ] )
	        def enteredCount = getTableCount( sqlObject, sqlCountString, [ 'ENTERED', fromString ] )
	        def cleanCount   = getTableCount( sqlObject, sqlCountString, [ 'CLEAN', fromString ] )
	    then:
	        enteredCount == 7
	        cleanCount   == 0

	    when:
	        isw.runClam( sqlObject, realClamAVClient )
	        enteredCount = getTableCount( sqlObject, sqlCountString, [ 'ENTERED', fromString ] )
	        cleanCount   = getTableCount( sqlObject, sqlCountString, [ 'CLEAN', fromString ] )
	    then:
	        1 == 1
	        enteredCount == 0
	        cleanCount   == 7
	}
	
    @Ignore
	@Requires({ properties[ 'clam.live.daemon' ] != 'true' })
	def "test cleaning messages with mocks"() {
	    println "\n--- Starting test ${name.methodName}"
	    def clamavMock      = Mock( ClamAVClient )
	    def inputStreamMock = Mock( InputStream )
	    byte[] outputMock   = "OK".getBytes()
	    def numTimes = 5
	    when:
	        numTimes.times { insertIntoMailSpoolIn( 'ENTERED' ) }
	        insertIntoMailSpoolIn( 'ENTERED', gwString + '@' + domainList[ 0 ] + ',' + jaString + '@' + domainList[ 0 ] )
	        insertIntoMailSpoolIn( 'ENTERED', gwString + '@' + domainList[ 0 ] + ',' +  getRandomString() + '@' + domainList[ 0 ] )
	        numTimes += 2
	        def enteredCount = getTableCount( sqlObject, sqlCountString, [ 'ENTERED', fromString ] )
	        def cleanCount   = getTableCount( sqlObject, sqlCountString, [ 'CLEAN', fromString ] )
	    then:
	        enteredCount == numTimes
	        cleanCount == 0
	    when:
	        clamavMock.scan( _ ) >> outputMock
	        outputMock.toString() >> "Hello"
	        isw.runClam( sqlObject, clamavMock )
	        enteredCount = getTableCount( sqlObject, sqlCountString, [ 'ENTERED', fromString ] )
	        cleanCount   = getTableCount( sqlObject, sqlCountString, [ 'CLEAN', fromString ] )
	    then:
	        _ * ClamAVClient.isCleanReply( outputMock ) // subscriber.receive("hello")
	        1 == 1
	        enteredCount == 0
	        cleanCount   == numTimes
	}
	
    @Ignore
	@Requires({ properties[ 'clam.live.daemon' ] != 'true' })
	def "test unclean messages with mocks"() {
	    println "\n--- Starting test ${name.methodName}"
	    def clamavMock      = Mock( ClamAVClient )
	    def inputStreamMock = Mock( InputStream )
	    byte[] outputMock   = "FOUND".getBytes()
	    def numTimes = 6
	    when:
	        numTimes.times { insertIntoMailSpoolIn( 'ENTERED' ) }
	        def enteredCount = getTableCount( sqlObject, sqlCountString, [ 'ENTERED', fromString ] )
	        def uncleanCount = getTableCount( sqlObject, sqlCountString, [ 'UNCLEAN', fromString ] )
	    then:
	        enteredCount == numTimes
	        uncleanCount == 0
	        
	    when:
	        clamavMock.scan( _ ) >> outputMock
	        outputMock.toString() >> "Hello"
	        isw.runClam( sqlObject, clamavMock )
	        enteredCount = getTableCount( sqlObject, sqlCountString, [ 'ENTERED', fromString ] )
	        uncleanCount = getTableCount( sqlObject, sqlCountString, [ 'UNCLEAN', fromString ] )
	    then:
	        _ * ClamAVClient.isCleanReply( outputMock ) 
	        1 == 1
	        enteredCount == 0
	        uncleanCount == numTimes
	}
	
    @Ignore
	def "test transferring clean messages"() {
	    when:
	        def transferredCount = getTableCount( sqlObject, sqlCountString, [ 'TRANSFERRED', fromString ] )
	        def cleanCount       = getTableCount( sqlObject, sqlCountString, [ 'CLEAN', fromString ] )
	        def storeCount       = getTableCount( sqlObject, sqlCountStoreString, [ fromString ] )
	    then:
	        transferredCount == 0
	        cleanCount == 7
	        storeCount == 0

	    when:
	        isw.moveCleanMessages( sqlObject )
	        transferredCount = getTableCount( sqlObject, sqlCountString, [ 'TRANSFERRED', fromString ] )
	        cleanCount       = getTableCount( sqlObject, sqlCountString, [ 'CLEAN', fromString ] )
	        storeCount       = getTableCount( sqlObject, sqlCountStoreString, [ fromString ] )
	    then:
	        1 == 1
	        transferredCount == 7
	        cleanCount == 0
	        storeCount == 8
	}
	
    @Ignore
	def "test deleting transferred messages"() {
	    when:
	        def transferredCount = getTableCount( sqlObject, sqlCountString, [ 'TRANSFERRED', fromString ] )
	        def storeCount       = getTableCount( sqlObject, sqlCountStoreString, [ fromString ] )
	    then:
	        transferredCount == 7
	        storeCount       == 8

	    when:
	        isw.deleteTransferredMessages( sqlObject )
	        transferredCount = getTableCount( sqlObject, sqlCountString, [ 'TRANSFERRED', fromString ] )
	        storeCount       = getTableCount( sqlObject, sqlCountStoreString, [ fromString ] )
	    then:
	        1 == 1
	        transferredCount == 0
	        storeCount       == 8
	}
	
    @Ignore
	def "test deleting transferred messages if it is empty"() {
	    when:
	        def transferredCount = getTableCount( sqlObject, sqlCountString, [ 'TRANSFERRED', fromString ] )
	        def storeCount       = getTableCount( sqlObject, sqlCountStoreString, [ fromString ] )
	    then:
	        transferredCount == 0
	        storeCount       == 8

	    when:
	        isw.deleteTransferredMessages( sqlObject )
	        transferredCount = getTableCount( sqlObject, sqlCountString, [ 'TRANSFERRED', fromString ] )
	        storeCount       = getTableCount( sqlObject, sqlCountStoreString, [ fromString ] )
	    then:
	        1 == 1
	        transferredCount == 0
	        storeCount       == 8
	}
	
    @Ignore
	def "test delete unclean messages"() {
	    def numUnclean      = 5
	    def uncleanMessages = 0
	    when:
	        numUnclean.times { insertIntoMailSpoolIn( 'UNCLEAN' ) }
	        uncleanMessages = getTableCount( sqlObject, sqlCountString, [ 'UNCLEAN', fromString ] )
	        println "numUnclean == ${numUnclean} and uncleanMessages == ${uncleanMessages}"
	    then:
	        numUnclean != uncleanMessages
	    when:
	        isw.deleteUncleanMessages( sqlObject )
	        uncleanMessages = getTableCount( sqlObject, sqlCountString, [ 'UNCLEAN', fromString ] )
	    then:
	        uncleanMessages == 0
	        
	}

    def "test with different case"() {
        def fromStringB = getRandomString() + rString + "@" + getRandomString() + ".com"
        fromArray << fromStringB
        when:
            addUser( sqlObject, 'George', 'Warshington', "davidbruce${rString}".toString(), 'somePassword' )
            insertIntoMailSpoolIn( 'CLEAN', "DavidBruce${rString.toUpperCase()}".toString() + '@' + domainList[ 0 ] )
	        def transferredCount = getTableCount( sqlObject, sqlCountString, [ 'TRANSFERRED', fromString ] )
	        def cleanCount       = getTableCount( sqlObject, sqlCountString, [ 'CLEAN', fromString ] )
	        def storeCount       = getTableCount( sqlObject, sqlCountStoreString, [ fromString ] )
	    then:
	        transferredCount == 0
	        cleanCount       == 1
            storeCount       == 0
        
        when:
            isw.moveCleanMessages( sqlObject )
	        transferredCount = getTableCount( sqlObject, sqlCountString, [ 'TRANSFERRED', fromString ] )
	        cleanCount       = getTableCount( sqlObject, sqlCountString, [ 'CLEAN', fromString ] )
            storeCount       = getTableCount( sqlObject, sqlCountStoreString, [ fromString ] )
        then:
            transferredCount == 1
	        cleanCount       == 0
            storeCount       == 1
            
        // 'select count(*) from mail_spool_in where status_string = ? and from_address = ?'
    }

    def "test transferring a message with valid and non-existent user in different messages"() {
        def diffMessFromString = getRandomString() + rString + "@" + getRandomString() + ".com"
        fromArray << diffMessFromString
        when:
            def nonExistUser = getRandomString() + "@" + domainList[ 0 ]
            insertIntoMailSpoolIn( 'CLEAN', nonExistUser, diffMessFromString  )
            insertIntoMailSpoolIn( 'CLEAN', gwString + '@' + domainList[ 0 ], diffMessFromString  )
	        def transferredCount = getTableCount( sqlObject, sqlCountString, [ 'TRANSFERRED', diffMessFromString ] )
	        def cleanCount       = getTableCount( sqlObject, sqlCountString, [ 'CLEAN', diffMessFromString ] )
            def storeCount       = getTableCount( sqlObject, sqlCountStoreString, [ diffMessFromString ] )
            def invalidCount     = getTableCount( sqlObject, sqlCountString, [ 'INBOUND_INVALID_USER', diffMessFromString ] )
	       
	    then:
	        transferredCount == 0
	        cleanCount       == 2
            invalidCount     == 0
            storeCount       == 0
        
        when:
            isw.moveCleanMessages( sqlObject )
	        transferredCount = getTableCount( sqlObject, sqlCountString, [ 'TRANSFERRED', diffMessFromString ] )
	        cleanCount       = getTableCount( sqlObject, sqlCountString, [ 'CLEAN', diffMessFromString ] )
            invalidCount     = getTableCount( sqlObject, sqlCountString, [ 'INBOUND_INVALID_USER', diffMessFromString ] )
            storeCount       = getTableCount( sqlObject, sqlCountStoreString, [ diffMessFromString ] )
            
        then:
            transferredCount == 1
	        cleanCount       == 0
            storeCount       == 1
            invalidCount     == 1
            
        // 'select count(*) from mail_spool_in where status_string = ? and from_address = ?'
    }

    def "test transferring a message with valid and non-existent user in same messge"() {
        def newFromString = getRandomString() + rString + "@" + getRandomString() + ".com"
        println "Here is newFromString: ${newFromString}"
        fromArray << newFromString
        when:
            def nonExistUser = getRandomString() + "@" + domainList[ 0 ]
            insertIntoMailSpoolIn( 'CLEAN', nonExistUser + "," + gwString + '@' + domainList[ 0 ], newFromString  )
	        def transferredCount = getTableCount( sqlObject, sqlCountString, [ 'TRANSFERRED', newFromString ] )
	        def cleanCount       = getTableCount( sqlObject, sqlCountString, [ 'CLEAN', newFromString ] )
            def storeCount       = getTableCount( sqlObject, sqlCountStoreString, [ newFromString ] )
            def invalidCount     = getTableCount( sqlObject, sqlCountString, [ 'INBOUND_INVALID_USER', newFromString ] )
	       
	    then:
	        transferredCount == 0
	        cleanCount       == 1
            invalidCount     == 0
            storeCount       == 0
        
        when:
            isw.moveCleanMessages( sqlObject )
	        transferredCount = getTableCount( sqlObject, sqlCountString, [ 'TRANSFERRED', newFromString ] )
	        cleanCount       = getTableCount( sqlObject, sqlCountString, [ 'CLEAN', newFromString ] )
            invalidCount     = getTableCount( sqlObject, sqlCountString, [ 'INBOUND_INVALID_USER', newFromString ] )
            storeCount       = getTableCount( sqlObject, sqlCountStoreString, [ newFromString ] )
            
        then:
            transferredCount == 0
	        cleanCount       == 0
            storeCount       == 1
            invalidCount     == 1
            
        // 'select count(*) from mail_spool_in where status_string = ? and from_address = ?'
    }

	@Ignore
	def "always ignore"() {
	    expect:
	        1 == 1
	}
	
} // line 246

