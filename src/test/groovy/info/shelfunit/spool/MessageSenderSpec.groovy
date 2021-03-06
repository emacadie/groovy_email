package info.shelfunit.spool

import spock.lang.Ignore
// import spock.lang.Requires
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
// import static info.shelfunit.mail.GETestUtils.getTableCount

import info.shelfunit.mail.meta.MetaProgrammer
// import info.shelfunit.smtp.command.EHLOCommand

@Stepwise
class MessageSenderSpec extends Specification {
    @Rule 
    TestName name = new TestName()
    
    def crlf = "\r\n"
    static sqlObject
    static config
    static domainList   = [] // [ 'shelfunit.info', 'groovy-is-groovy.org' ]
    static rString      = getRandomString()
    static gwString     = 'gw' + rString
    static jaString     = 'ja' + rString
    static tjString     = 'tj' + rString
    static fromString   = ''
    static gwBase64Hash = getBase64Hash( gwString, 'somePassword' )
    static uuidList     = []
    static params       = []
    static mSender      = new MessageSender()
    static sqlCountString      = 'select count(*) from mail_spool_out where status_string = ? and from_address = ?'
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
        config     = ConfigHolder.instance.getConfObject()
        domainList = this.buildServerList( config  )
        fromString = gwString + '@' + domainList[ 1 ]

    }     // run before the first feature method
    
    def cleanupSpec() {
        sqlObject.execute "DELETE FROM email_user where username in ( ?, ?, ? )", [ gwString, jaString, tjString ]
        sqlObject.execute "DELETE FROM mail_spool_in where from_address = ?", [ fromString ]
        println "about to delete from mail_spool_out where from address in ${fromString}, ${gwString}, ${jaString}, ${tjString} "
        sqlObject.execute "DELETE FROM mail_spool_out where from_address in (?, ?, ?, ?)", [ fromString, gwString, jaString, tjString ]
        sqlObject.close()
    }   // run after the last feature method

    def buildServerList( def argConfig ) {
        def returnList     = []
        def tempServerList = [ argConfig.smtp.fq.server.name ]
        argConfig.smtp.server.name.isEmpty() ?: ( tempServerList += argConfig.smtp.server.name )
        argConfig.smtp.other.domains.isEmpty() ?: ( tempServerList += argConfig.smtp.other.domains )
        tempServerList.collect{ returnList << it.toLowerCase() }
        println "Here is returnList: ${returnList}"
        return returnList
    }
    
    def addUsers() {
        addUser( sqlObject, 'George', 'Washington', gwString, 'somePassword' )
        addUser( sqlObject, 'John',   'Adams',      jaString, 'somePassword' )
        addUser( sqlObject, 'Jack',   "O'Neill",    tjString, 'somePassword' )
    }
        
    def insertIntoMailSpoolOut( status, toAddress, message = getRandomString( 500 ), uuid = UUID.randomUUID() ) {
        params.clear()
        params << uuid // id
        params << gwString + '@' + domainList[ 1 ] // from_address, 
        params << gwString // from_username, 
        params << domainList[ 1 ] // from_domain,
        params << toAddress    // to_address_list
        params << message      // text_body,
        params << status       // status_string,  
        params << gwBase64Hash // base_64_hash
        println "entering message from ${gwString} to ${toAddress}" 
        sqlObject.execute "insert into mail_spool_out( " +
            "id, from_address, from_username, from_domain, " +
            "to_address_list, text_body, status_string, " +
            "base_64_hash ) values (?, ?, ?, ?, ?, ?, ?, ?)", 
            params
    }
    
    def getMessage( uuid ) {
        return sqlObject.firstRow( 'select * from mail_spool_out where id = ?', [ uuid ] )
    }

        def "test domain names are correct"() {
        expect:
            domainList[ 0 ] == "mail.neutral.nt"
            domainList[ 1 ] == "neutral.nt"
    }

    def "first mail test"() {
        setup:
            def uuid          = UUID.randomUUID()
            def messageString = 'q' * 500
            def message       = this.insertIntoMailSpoolOut( 'ENTERED', 'oneill@stargate.mil', messageString, uuid )
            def row           = this.getMessage( uuid )
        when:
            def bString = "220 stargte.mil Simple Mail Transfer Service Ready\r\n" + 
            "250-Hello ${domainList[ 0 ]}\r\n" +
            "250-8BITMIME\r\n"   +
            "250-AUTH PLAIN\r\n" + 
            "250 HELP\r\n"  +
            "250 OK${crlf}" +          // MAIL FROM
            "250 OK${crlf}" +          // RCPT TO
            "354 cha-cha-cha${crlf}" + // DATA
            "250 OK${crlf}" +          // After DATA
            "221 stargate.mil ending transmission${crlf}"
            byte[] data = bString.getBytes()
    
            InputStream input   = new ByteArrayInputStream( data )
            OutputStream output = new ByteArrayOutputStream() 
            
            mSender.doWork( input, output, row, 'stargate.mil', [ 'oneill' ], domainList[ 1 ], domainList[ 0 ] )
            println "Here is output.toString(): ${output.toString()}"
        then:
	        output.toString() == "EHLO ${domainList[ 0 ]}\r\n" +
                "MAIL FROM:<${gwString}@${domainList[ 1 ]}>\r\n" +
                "RCPT TO:<oneill@stargate.mil>\r\n" +
                "DATA\r\n" +
                "${messageString}\r\n" + 
                ".\r\n" +
                "QUIT\r\n" 
    } // "first test"() 

    @Ignore
    def "test handle DSN"() {
        setup:
            def uuid          = UUID.randomUUID()
            def messageString = 'q' * 500
            def message       = this.insertIntoMailSpoolOut( 'ENTERED', 'oneill@stargate.mil', messageString, uuid )
            def row           = this.getMessage( uuid )
        when:
            def bString = "220 stargte.mil Simple Mail Transfer Service Ready\r\n" + 
            "250-Hello ${domainList[ 1 ]}\r\n" +
            "250-DSN\r\n" +
            "250-8BITMIME\r\n"   +
            "250-AUTH PLAIN\r\n" + 
            "250 HELP\r\n"  +
            "250 OK${crlf}" +          // MAIL FROM
            "250 OK${crlf}" +          // RCPT TO
            "354 cha-cha-cha${crlf}" + // DATA
            "250 OK${crlf}" +          // After DATA
            "221 stargate.mil ending transmission${crlf}"
            byte[] data = bString.getBytes()
    
            InputStream input   = new ByteArrayInputStream( data )
            OutputStream output = new ByteArrayOutputStream() 
            
            mSender.doWork( input, output, row, 'stargate.mil', [ 'oneill' ], domainList[ 1 ], domainList[ 0 ] )
            println "Here is output.toString(): ${output.toString()}"
        then:
	        output.toString() == "EHLO ${domainList[ 0 ]}\r\n"   + 
                "MAIL FROM:<${gwString}@${domainList[ 1 ]}>\r\n" +
                "RCPT TO:<oneill@stargate.mil> NOTIFY=NEVER\r\n" +
                "DATA\r\n" +
                "${messageString}\r\n" + 
                ".\r\n" +
                "QUIT\r\n" 
    } // "test handle DSN"

    def "test send lower case"() {
        setup:
            def uuid          = UUID.randomUUID()
            def messageString = 'q' * 500
            def message       = this.insertIntoMailSpoolOut( 'ENTERED', 'ONeill@stargate.mil'.toUpperCase(), messageString, uuid )
            def row           = this.getMessage( uuid )
        when:
            def bString = "220 stargte.mil Simple Mail Transfer Service Ready\r\n" + 
            "250-Hello ${domainList[ 1 ]}\r\n" +
            // "250-DSN\r\n" +
            "250-8BITMIME\r\n"   +
            "250-AUTH PLAIN\r\n" + 
            "250 HELP\r\n"  +
            "250 OK${crlf}" +          // MAIL FROM
            "250 OK${crlf}" +          // RCPT TO
            "354 cha-cha-cha${crlf}" + // DATA
            "250 OK${crlf}" +          // After DATA
            "221 stargate.mil ending transmission${crlf}"
            byte[] data = bString.getBytes()
    
            InputStream input   = new ByteArrayInputStream( data )
            OutputStream output = new ByteArrayOutputStream() 
            
            mSender.doWork( input, output, row, 'stargate.mil', [ 'ONeill' ], domainList[ 1 ], domainList[ 0 ] )
            println "Here is output.toString(): ${output.toString()}"
            
        then:
	        output.toString() == "EHLO ${domainList[ 0 ]}\r\n"   +
                "MAIL FROM:<${gwString}@${domainList[ 1 ]}>\r\n" +
                // "RCPT TO:<oneill@stargate.mil> NOTIFY=NEVER\r\n" +
                "RCPT TO:<oneill@stargate.mil>\r\n" +
                "DATA\r\n" +
                "${messageString}\r\n" + 
                ".\r\n" +
                "QUIT\r\n" 
    } // "test handle DSN"
	
	@Ignore
	def "always ignore"() {
	    expect:
	        1 == 1
	}
	
} // line 246

