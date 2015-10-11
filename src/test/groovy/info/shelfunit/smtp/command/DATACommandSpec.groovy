package info.shelfunit.smtp.command

import spock.lang.Specification

import org.junit.Rule
import org.junit.rules.TestName

import info.shelfunit.mail.meta.MetaProgrammer

import groovy.sql.Sql

import org.apache.shiro.crypto.hash.Sha512Hash

class DATACommandSpec extends Specification {
    
    def crlf = "\r\n"
    static domainList = [ 'shelfunit.info', 'groovy-is-groovy.org' ]
    static sql
    static dataCommand
    static hamilton = 'alexander@shelfunit.info'
    static gwShelf  = 'george.washington@shelfunit.info'
    static jAdamsShelf = 'john.adams@shelfunit.info'
    static jackShell   = 'oneill@shelfunit.info'
    static gwGroovy    = 'george.washington@groovy-is-groovy.org'
    static jaGroovy    = 'john.adams@groovy-is-groovy.org'
    static jackGroovy  = 'oneill@groovy-is-groovy.org'
    static resultSetEMR = [ 'EHLO', 'MAIL', 'RCPT' ] as Set
    static resultSetEM  = [ 'EHLO', 'MAIL' ] as Set 

    @Rule 
    TestName name = new TestName()
    
    def setup() {
        println "\n--- Starting test ${name.methodName}"
    }          // run before every feature method
    def cleanup() {}        // run after every feature method
    
    def setupSpec() {
        MetaProgrammer.runMetaProgramming()
        def db = [ url: "jdbc:postgresql://${System.properties[ 'host_and_port' ]}/${System.properties[ 'dbname' ]}",
        user: System.properties[ 'dbuser' ], password: System.properties[ 'dbpassword' ], driver: 'org.postgresql.Driver' ]
        sql = Sql.newInstance( db.url, db.user, db.password, db.driver )
        // this.addUsers()
        dataCommand = new DATACommand(  )
    }     // run before the first feature method
    
    def cleanupSpec() {
        // sql.execute "DELETE FROM email_user"
        sql.close()
    }   // run after the last feature method
   
    def addUsers() {
        def numIterations = 10000
        def salt = 'you say your password tastes like chicken? Add salt!'
        def atx512 = new Sha512Hash( 'somePassword', salt, 1000000 )
        def params = [ 'gw001', atx512.toBase64(), 'SHA-512', numIterations, 'George', 'Washington', 0 ]
        sql.execute 'insert into  email_user( username, password_hash, password_algo, iterations, first_name, last_name, version ) values ( ?, ?, ?, ?, ?, ?, ? )', params
        
        params = [ 'ja002', atx512.toBase64(), 'SHA-512', numIterations, 'John', 'Adams', 0 ]
        sql.execute 'insert into  email_user( username, password_hash, password_algo, iterations, first_name, last_name, version ) values ( ?, ?, ?, ?, ?, ?, ? )', params
        
        params = [ 'tj003', atx512.toBase64(), 'SHA-512', numIterations, 'Thomas', 'Jefferson', 0 ]
        sql.execute 'insert into  email_user( username, password_hash, password_algo, iterations, first_name, last_name, version ) values ( ?, ?, ?, ?, ?, ?, ? )', params
        // sql.commit()
    }
    
    def "test command with extra stuff"() {
        def bufferMapArg = [ forwardPath:[ 'alexander@shelfunit.info', 'george.washington@shelfunit.info' ], reversePath: 'oneill@stargate.mil' ]
        def prevCommandSetArg = [ 'EHLO', 'MAIL', 'RCPT' ] as Set
        when:
            def resultMap = dataCommand.process( 'DATA hello', prevCommandSetArg, bufferMapArg )
        then:
            resultMap.prevCommandSet == prevCommandSetArg
            resultMap.bufferMap == bufferMapArg
            resultMap.resultString == "501 Command not in proper form"
    }

	def "test handling wrong command"() {
	    def bufferMapArg = [ forwardPath:[ 'alexander@shelfunit.info', 'george.washington@shelfunit.info' ], reversePath: 'oneill@stargate.mil' ]
        def prevCommandSetArg = [ 'EHLO', 'MAIL', 'RCPT' ] as Set
        when:
            def resultMap = dataCommand.process( 'RCPT', prevCommandSetArg, bufferMapArg )
        then:
            resultMap.prevCommandSet == prevCommandSetArg
            resultMap.bufferMap == bufferMapArg
            resultMap.resultString == "503 Bad sequence of commands"
	}
	
	def "test commands in wrong order"() {
	    def bufferMapArg = [ forwardPath:[ 'alexander@shelfunit.info', 'george.washington@shelfunit.info' ], reversePath: 'oneill@stargate.mil' ]
        def prevCommandSetArg = [ 'EHLO', 'MAIL' ] as Set
        when:
            def resultMap = dataCommand.process( 'DATA', prevCommandSetArg, bufferMapArg )
        then:
            resultMap.prevCommandSet == prevCommandSetArg
            resultMap.bufferMap == bufferMapArg
            resultMap.resultString == "503 Bad sequence of commands"
	}

	def "test happy path"() {
	    
	    def bufferMapArg = [ forwardPath:[ 'alexander@shelfunit.info', 'george.washington@shelfunit.info' ], reversePath: 'oneill@stargate.mil' ]
        def prevCommandSetArg = [ 'EHLO', 'MAIL', 'RCPT' ] as Set
        when:
            def resultMap = dataCommand.process( 'DATA', prevCommandSetArg, bufferMapArg )
        then:
            resultMap.prevCommandSet == [ 'EHLO', 'MAIL', 'RCPT', 'DATA' ] as Set
            resultMap.bufferMap == bufferMapArg
            resultMap.resultString == "354 Start mail input; end with <CRLF>.<CRLF>"
	}

}

