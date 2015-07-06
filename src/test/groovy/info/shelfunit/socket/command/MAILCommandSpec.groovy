package info.shelfunit.socket

import spock.lang.Specification

import org.junit.Rule
import org.junit.rules.TestName

import info.shelfunit.mail.MailRunner
import info.shelfunit.socket.command.MAILCommand

import org.xbill.DNS.Address

import groovy.mock.interceptor.StubFor

class MAILCommandSpec extends Specification {
    
    def crlf = "\r\n"

    @Rule 
    TestName name = new TestName()
    
    def setup() {
        println "\n--- Starting test ${name.methodName}"
    }          // run before every feature method
    def cleanup() {}        // run after every feature method
    def setupSpec() {
        MailRunner.runMetaProgramming()
    }     // run before the first feature method
    
    def cleanupSpec() {}   // run after the last feature method

	def "test handling wrong command"() {
	    def mailCommand = new MAILCommand()
	    
	    when:
	        def resultMap = mailCommand.process( "MAIL FROM:<oneill@stargate.mil>", ['RCPT'], [:] )
	        def mailResponse = resultMap.resultString + crlf 
	    then:
	        mailResponse == "503 Bad sequence of commands\r\n"
	        resultMap.prevCommandList == [ "RCPT" ]
	}
	
	def "test happy path"() {
	    def mailCommand = new MAILCommand()
	    
	    when:
	        def resultMap = mailCommand.process( "MAIL FROM:<oneill@stargate.mil>", ['EHLO'], [:] )
	        def mailResponse = resultMap.resultString + crlf 
	        def bMap = resultMap.bufferMap
	    then:
	        mailResponse == "250 OK\r\n"
	        resultMap.prevCommandList == [ "EHLO", "MAIL" ]
	        bMap.reversePath == 'oneill@stargate.mil'
	}
	/*
	def "test handling HELO"() {
	    def ehloCommand = new EHLOCommand()
	    
	    def domain = "hot-groovy.com"
	    when:
	        def resultMap = ehloCommand.process( "HELO ${domain}", [], [:] )
	        def ehloResponse = resultMap.resultString + crlf
	    then:
	        ehloResponse == "250 Hello ${domain}\r\n"
	}
	*/
	/*
	def "test handling domain more than 255 char"() {
	    def ehloCommand = new EHLOCommand()
	    
	    def longString = ( "f" * 252 ) + '.com'
	    when:
	        def resultMap = ehloCommand.process( "EHLO ${longString}", [], [:] )
	        def ehloResponse = resultMap.resultString + crlf 
	    then:
	        ehloResponse == "501 Domain name length beyond 255 char limit per RFC 3696\r\n"
	}
	*/
	/*
	def "test that command list and buffer map are cleared"() {
	    def domain = "www.groovymail.org"
	    def ehloCommand = new EHLOCommand()
	    
	    when:
	        def prevCommandList = [ 'Get the hell off my ship', 'keep us under the radar, Wash' ]
	        def bufferMap = [ name:'Jayne', location: 'bunk' ]
	    then:
	        prevCommandList.size() == 2
	        bufferMap.size() == 2
	    when:
	        def resultMap = ehloCommand.process( "EHLO ${domain}", prevCommandList, bufferMap )
	        def ehloResponse = resultMap.resultString + crlf 
	        def newList = resultMap.prevCommandList
	        def newMap = resultMap.bufferMap
	    then:
	        ehloResponse == "250-Hello ${domain}\n" +
	        "250 HELP\r\n"
	        newList.size() == 1
	        newList[ 0 ] == "EHLO"
	        newMap.size() == 0
	}
	*/
	/*
	def "test getting address"() {
	    def serverName = "www.groovymail.org"
	    def ehloCommand = new EHLOCommand()
	    def inetAddress = Stub( InetAddress )
	    def address = GroovyStub( Address )
	    address.getByName(_) >> inetAddress
	    inetAddress.hostAddress >> 'X.X.X.X'
	    def result = ehloCommand.processDomain( "XYZ" )
	    println "here is result: ${result}"
	    expect:
	        result == 'X.X.X.X'
	        1 == 1
	}
	*/
	/*
	def "test without mocks or stubs"() {
	    when:
            def domain = "www.shelfunit.info"
            def ehloCommand = new EHLOCommand()
            def result = ehloCommand.processDomain( domain )
            println "here is result: ${result}"
	    then:
	        result == '45.33.18.182'
	}
	*/
	// this works even if not connected to internet
	/*
	def "test good domain with mocks or stubs"() {
	    when:
            def domain = "www.shelfunit.info"
            def ehloCommand = new EHLOCommand()
            def inetAddress = GroovyStub( InetAddress )
            inetAddress.hostAddress >> '45.33.18.182'
            def result = ''
            def stub = new StubFor( Address )      
            stub.demand.with {                  
                getByName{ inetAddress }
            }
            stub.use {                          
                result = ehloCommand.processDomain( domain )
            }
            println "here is result: ${result}"
	    then:
	        stub.expect.verify() 
	        result == '45.33.18.182'
	}
	*/
}

