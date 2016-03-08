package info.shelfunit.smtp.command

// import spock.lang.Ignore
import spock.lang.Specification

import org.junit.Rule
import org.junit.rules.TestName

import info.shelfunit.mail.ConfigHolder
import info.shelfunit.mail.meta.MetaProgrammer
import static info.shelfunit.mail.GETestUtils.addUser
import static info.shelfunit.mail.GETestUtils.getRandomString

import groovy.util.logging.Slf4j 

@Slf4j
class MSSGCommandSpec extends Specification {
    
    def crlf = "\r\n"
    static sql
    static mssgCommand
    static rString = getRandomString()
    static georgeW = 'gw' + rString
    static johnA   = 'ja' + rString
    static jackO   = 'jo' + rString
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
        sql = ConfigHolder.instance.getSqlObject() 
        this.addUsers()
        mssgCommand = new MSSGCommand( sql, domainList )
    }     // run before the first feature method
    
    def cleanupSpec() {
        sql.execute "DELETE FROM email_user where username in ( ?, ?, ? )", [ georgeW, johnA, jackO ]
        sql.close()
    }   // run after the last feature method
   
    def addUsers() {
        addUser( sql, 'George', 'Washington', georgeW, 'somePassword' )
        addUser( sql, 'John', 'Adams', johnA, 'somePassword' )
        addUser( sql, 'Jack', "O'Neill", jackO, 'somePassword' )
    }
    
	def "test handling wrong command"() {
	    def bufferMapArg = [ forwardPath:[ 'alexander@shelfunit.info', georgeW + '@shelfunit.info' ], reversePath: 'oneillMSSG@stargate.mil' ]
        def prevCommandSetArg = [ 'EHLO', 'MAIL', 'RCPT' ] as Set
	    when:
	        def resultMap = mssgCommand.process( "The next meeting of the board of directors will be on Tuesday.\nJohn.", prevCommandSetArg, [ forwardPath:  [ hamilton ] ] )
	        def mailResponse = resultMap.resultString + crlf 
	    then:
	        mailResponse == "503 Bad sequence of commands\r\n"
	        resultMap.prevCommandSet == prevCommandSetArg
	}
	
	// @Ignore
	def "test handling a message"() {
	    def bufferMapArg = [ forwardPath:[ johnA + '@shelfunit.info', georgeW + '@shelfunit.info' ], reversePath: jackO + '@stargate.mil' ]
	    def uuidSet = [] as Set
	    bufferMapArg.forwardPath.size().times() {
            uuidSet << UUID.randomUUID() 
        }
        def countResult
        def qList = []
        ( 1..uuidSet.size() ).each { qList << '?' }
        def qCString = qList.join( ',' )
        def sqlString = 'select count(*) from mail_store where id in (' + qCString + ')'
        when:
            countResult = sql.firstRow( sqlString, uuidSet as List )
        then:
            countResult.count == 0
            
        def theMessage = "The next meeting of the board of directors will be on Tuesday.\nJohn."
        def prevCommandSetArg = [ 'EHLO', 'MAIL', 'RCPT' ] as Set
	    when:
	        def mailResponse = mssgCommand.addMessageToDatabase( theMessage, bufferMapArg, uuidSet ) 
	        countResult = sql.firstRow( sqlString, uuidSet as List )
	    then:
	        mailResponse == "250 OK"
	        countResult.count == uuidSet.size()
	}
	
	// @Ignore
	def "test handling a BAD message"() {
	    def bufferMapArg = [ forwardPath:[ johnA + '@shelfunit.info', georgeW + '@shelfunit.info' , 'chumba-wumba@shelfunit.info' ], reversePath: jackO + '@stargate.mil' ]
	    def uuidSet = [] as Set
	    bufferMapArg.forwardPath.size().times() {
            uuidSet << UUID.randomUUID() 
        }
        def countResult
        def qList = []
        ( 1..uuidSet.size() ).each { qList << '?' }
        def qCString = qList.join( ',' )
        def sqlString = 'select count(*) from mail_store where id in (' + qCString + ')'
        when:
            countResult = sql.firstRow( sqlString, uuidSet as List )
        then:
            countResult.count == 0
            
        def theMessage = "The next meeting of the board of directors will be on Friday.\nStay Groovy\nJohn."
        def prevCommandSetArg = [ 'EHLO', 'MAIL', 'RCPT' ] as Set
	    when:
	        def mailResponse = mssgCommand.addMessageToDatabase( theMessage, bufferMapArg, uuidSet ) 
	    then:
	        mailResponse == "500 Something went wrong"
	    when:
	        countResult = sql.firstRow( sqlString, uuidSet as List )
	    then:
	        countResult.count == 0
	}
	
	def "test getting the data"() {
	    sql.eachRow( 'select * from mail_store' ) { mailItem ->
	        println mailItem.id.toString()
            println "mailItem.text_body is a ${mailItem.text_body.getClass().getName()}"
            println "mailItem.text_body: ${mailItem.text_body}"
            println "mailItem.username: ${mailItem.username}\n-------------------------------------------------"
        }
	    expect:
	        5 == 5
	}

} // line 150

