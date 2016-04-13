package info.shelfunit.spool

import spock.lang.Ignore
import spock.lang.Requires
import spock.lang.Specification
import spock.lang.Stepwise

import java.io.InputStream
import java.io.OutputStream

import org.junit.Rule
import org.junit.rules.TestName

import info.shelfunit.mail.ConfigHolder
import static info.shelfunit.mail.GETestUtils.addUser
import static info.shelfunit.mail.GETestUtils.getBase64Hash
import static info.shelfunit.mail.GETestUtils.getRandomString
import static info.shelfunit.mail.GETestUtils.getTableCount

import info.shelfunit.mail.meta.MetaProgrammer
import info.shelfunit.smtp.command.EHLOCommand

@Stepwise
class MessageSenderSpec extends Specification {
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
    static config
    static uuidList = []
    static params = []
    static mSender = new MessageSender()
    static sqlCountString = 'select count(*) from mail_spool_out where status_string = ? and from_address = ?'
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
    
    def insertIntoMailSpoolOut( uuid, status, toAddress, message ) {
        params.clear()
        params << uuid
        params << gwString + '@' + domainList[ 0 ]
        params << toAddress
        params << message
        params << 'ENTERED'
        params << gwBase64Hash
        sql.execute 'insert into mail_spool_out( id, from_address, to_address_list, text_body, status_string, base_64_hash ) values (?, ?, ?, ?, ?, ?)', params
    }
    
    def getMessage( uuid ) {
        return sql.firstRow( 'select * from mail_spool_out where id = ?', [ uuid ] )
    }

    def "first test"() {
        setup:
            def uuid = UUID.randomUUID()
            def messageString = 'q' * 500
            def message = this.insertIntoMailSpoolOut( uuid, 'ENTERED', 'oneill@stargate.mil', messageString )
            def row = this.getMessage( uuid )
        when:
            def bString = "220 stargte.mil Simple Mail Transfer Service Ready\r\n" + 
            "250-Hello ${domainList[ 0 ]}\r\n" +
            "250-8BITMIME\r\n" +
            "250-AUTH PLAIN\r\n" + 
            "250 HELP\r\n" +
            "250 OK${crlf}" + // MAIL FROM
            "250 OK${crlf}" + // RCPT TO
            "354 cha-cha-cha${crlf}" + // DATA
            "250 OK${crlf}" +          // After DATA
            "221 stargate.mil ending transmission${crlf}"
            byte[] data = bString.getBytes()
    
            InputStream input = new ByteArrayInputStream( data )
            OutputStream output = new ByteArrayOutputStream() 
            
            mSender.doWork( input, output, row, 'stargate.mil', [ 'oneill@stargate.mil' ], domainList[ 0 ] )
            
        then:
	        output.toString() == "EHLO ${domainList[ 0 ]}\r\n" +
                "MAIL FROM:<${gwString}@${domainList[ 0 ]}>\r\n" +
                "RCPT TO:<oneill@stargate.mil>\r\n" +
                "DATA\r\n" +
                "${messageString}\r\n" + 
                ".\r\n" +
                "QUIT\r\n" 
    }
	
	@Ignore
	def "always ignore"() {
	    expect:
	        1 == 1
	}
	
} // line 246

