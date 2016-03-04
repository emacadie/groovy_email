package info.shelfunit.postoffice.command

import java.sql.Timestamp

import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Stepwise
// import spock.lang.Unroll

import org.junit.Rule
import org.junit.rules.TestName

import info.shelfunit.mail.meta.MetaProgrammer
import info.shelfunit.mail.ConfigHolder

import org.apache.shiro.crypto.hash.Sha512Hash
import static info.shelfunit.mail.GETestUtils.getBase64Hash

import groovy.sql.Sql
import groovy.util.logging.Slf4j 

@Slf4j
@Stepwise
class DELECommandSpec extends Specification {
    
    def crlf = "\r\n"
    static domainList = [ 'shelfunit.info', 'groovy-is-groovy.org' ]
    static sql
    static iterations = 10000
    static deleCommand
    static somePassword = 'somePassword'
    static theTimestamp
    static gwDELE  = 'gwdele' // @shelfunit.info'
    static jaDELE = 'jadele' // @shelfunit.info'
    static joDELE = 'jodele' // @shelfunit.info'
    static uuidA = UUID.randomUUID()
    static uuidB = UUID.randomUUID()
    static uuidC = UUID.randomUUID()
    static msgA = 'aq' * 10
    static msgB = 'bq' * 11
    static msgC = 'cq' * 12
    
    @Rule 
    TestName name = new TestName()
    
    def setup() {
        println "\n--- Starting test ${name.methodName}"
    }          // run before every feature method
    def cleanup() {}        // run after every feature method
    
    def setupSpec() {
        MetaProgrammer.runMetaProgramming()
        
        ConfigHolder.instance.setConfObject( "src/test/resources/application.test.conf" )
        def conf = ConfigHolder.instance.getConfObject()
        log.info "conf is a ${conf.class.name}"
        def db = [ url: "jdbc:postgresql://${conf.database.host_and_port}/${conf.database.dbname}",
        user: conf.database.dbuser, password: conf.database.dbpassword, driver: conf.database.driver ]
        log.info "db is a ${db.getClass().name}"
        sql = Sql.newInstance( db.url, db.user, db.password, db.driver )
        this.addUsers()
        deleCommand = new DELECommand( sql )
        
    }     // run before the first feature method
    
    def cleanupSpec() {
        sql.execute "DELETE FROM email_user where username in ( ${gwDELE}, ${jaDELE}, ${joDELE} )"
        sql.close()
    }   // run after the last feature method
    
    def addUsers() {
        def params = [ gwDELE, ( new Sha512Hash( somePassword, gwDELE, iterations ).toBase64() ), 'SHA-512', iterations, getBase64Hash( gwDELE, somePassword ), 'George', 'Washington', 0 ]
        sql.execute 'insert into  email_user( username, password_hash, password_algo, iterations, base_64_hash, first_name, last_name, version ) values ( ?, ?, ?, ?, ?, ?, ?, ? )', params
        
        params = [ jaDELE, ( new Sha512Hash( somePassword, jaDELE, iterations ).toBase64() ), 'SHA-512', iterations, getBase64Hash( gwDELE, somePassword ), 'John', 'Adams', 0 ]
        sql.execute 'insert into  email_user( username, password_hash, password_algo, iterations, base_64_hash, first_name, last_name, version ) values ( ?, ?, ?, ?, ?, ?, ?, ? )', params
        
        params = [ joDELE, ( new Sha512Hash( somePassword, joDELE, iterations ).toBase64() ), 'SHA-512', iterations, getBase64Hash( gwDELE, somePassword ), 'Jack', "O'Neill", 0 ]
        sql.execute 'insert into  email_user( username, password_hash, password_algo, iterations, base_64_hash, first_name, last_name, version ) values ( ?, ?, ?, ?, ?, ?, ?, ? )', params
        
        // sql.commit()
        this.addMessage( uuidA, gwDELE, msgA )
        this.addMessage( uuidB, gwDELE, msgB )
        this.addMessage( uuidC, gwDELE, msgC )
        theTimestamp = Timestamp.create()
    }
    
    def addMessage( uuid, userName, messageString ) {
        def toAddress = "${userName}@${domainList[ 0 ]}".toString()
        def params = [ uuid, userName, 'hello@test.com', toAddress, messageString ]
        sql.execute 'insert into mail_store(id, username, from_address, to_address, text_body) values (?, ?, ?, ?, ?)', params
    }
    
    // @Ignore
    def "test uuid list"() {
        def uuidList = []
        def uuid = UUID.randomUUID()
        def totalMessageSizeTest = msgA.size() + msgB.size() + msgC.size()
        def bufferInputMap = [:]
        def timestamp
        bufferInputMap.state = 'TRANSACTION'
        bufferInputMap.timestamp = theTimestamp
        def userInfo = [:]
	    userInfo.username = gwDELE
        bufferInputMap.userInfo = userInfo
        sleep( 2.seconds() )
        when:
            def resultMap = deleCommand.process( 'DELE', [] as Set, bufferInputMap )
        then:
            resultMap.resultString == "-ERR Command not in proper form"

        def messageStringB = 'aw' * 11
        def toAddress = "${gwDELE}@${domainList[ 0 ]}".toString()
        when:
            bufferInputMap = resultMap.bufferMap
            def params = [ UUID.randomUUID(), gwDELE, 'hello@test.com', toAddress, messageStringB ]
            sql.execute 'insert into mail_store(id, username, from_address, to_address, text_body) values (?, ?, ?, ?, ?)', params    
            def messageCount
        
            sql.eachRow( 'select count(*) from mail_store where username = ?', [ gwDELE ] ) { nextRow ->
                messageCount = nextRow.count
            }
        then:
            messageCount == 4
        when:
            resultMap = deleCommand.process( 'DELE', [] as Set, bufferInputMap )
        then:
            resultMap.resultString == "-ERR Command not in proper form"
	}

	def "test individual messages"() {
        def uuidList = []
        def uuid = UUID.randomUUID()
        def totalMessageSizeTest = msgA.size() + msgB.size() + msgC.size()
        def bufferInputMap = [:]
        def timestamp
        bufferInputMap.state = 'TRANSACTION'
        bufferInputMap.timestamp = theTimestamp
        def userInfo = [:]
	    userInfo.username = gwDELE
        bufferInputMap.userInfo = userInfo
        sleep( 2.seconds() )
        when:
            def resultMap = deleCommand.process( 'DELE 1', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "+OK message 1 deleted"  
            resultMap.bufferMap.deleteMap.containsKey( 1 ) == true
            resultMap.bufferMap.deleteMap.containsKey( 2 ) == false
            resultMap.bufferMap.deleteMap[ 1 ].toString() == uuidA.toString()
        
        when:
            resultMap = deleCommand.process( 'DELE 3', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "+OK message 3 deleted"  
            resultMap.bufferMap.deleteMap.containsKey( 3 ) == true
            resultMap.bufferMap.deleteMap.containsKey( 2 ) == false
            resultMap.bufferMap.deleteMap[ 3 ].toString() == uuidC.toString()
            
        when:
            resultMap = deleCommand.process( 'DELE 2', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "+OK message 2 deleted" 
            resultMap.bufferMap.deleteMap.containsKey( 2 ) == true
            resultMap.bufferMap.deleteMap[ 2 ].toString() == uuidB.toString()
        
        def messageStringB = 'aw' * 11
        def toAddress = "${gwDELE}@${domainList[ 0 ]}".toString()
        when:
            bufferInputMap = resultMap.bufferMap
            def params = [ UUID.randomUUID(), gwDELE, 'hello@test.com', toAddress, messageStringB ]
            sql.execute 'insert into mail_store(id, username, from_address, to_address, text_body) values (?, ?, ?, ?, ?)', params    
            def messageCount
        
            sql.eachRow( 'select count(*) from mail_store where username = ?', [ gwDELE ] ) { nextRow ->
                messageCount = nextRow.count
            }
        then:
            messageCount == 5
        
        when:
            resultMap = deleCommand.process( 'DELE 4', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString == "-ERR no such message, only 3 messages in maildrop"
            
        when:
            resultMap = deleCommand.process( 'DELE 1', [] as Set, bufferInputMap )
        then:
            resultMap.bufferMap.totalMessageSize == totalMessageSizeTest
            resultMap.resultString != "+OK ${msgA.size()} octets${crlf}" + "${msgA}${crlf}" + "."
            log.info "here is resultMap.resultString at end of test: ${resultMap.resultString}"
	}

}

