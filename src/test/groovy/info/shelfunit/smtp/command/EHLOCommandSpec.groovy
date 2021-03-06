package info.shelfunit.smtp.command

import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Stepwise
import spock.lang.Unroll

import org.junit.Rule
import org.junit.rules.TestName

import info.shelfunit.mail.meta.MetaProgrammer

import org.xbill.DNS.Address

import groovy.mock.interceptor.StubFor

@Stepwise
class EHLOCommandSpec extends Specification {
    
    def crlf = "\r\n"

    @Rule 
    TestName name = new TestName()
    
    def setup() {
        println "\n--- Starting test ${name.methodName}"
    }          // run before every feature method
    def cleanup() {}        // run after every feature method
    def setupSpec() {
        MetaProgrammer.runMetaProgramming()
    }     // run before the first feature method
    
    def cleanupSpec() {}   // run after the last feature method

	def "test handling EHLO"() {
	    def ehloCommand = new EHLOCommand()
	    
	    def domain = "hot-groovy.com"
	    when:
	        def resultMap    = ehloCommand.process( "EHLO ${domain}", [] as Set, [:] )
	        def ehloResponse = resultMap.resultString + crlf 
	    then:
	        ehloResponse == "250-Hello ${domain}\r\n" +
	        "250-8BITMIME\r\n" +
	        "250-AUTH PLAIN\r\n" + 
	        "250 HELP\r\n"
	        println "here is resultMap.prevCommandSet[0]: ${resultMap.prevCommandSet[0]} and it's a ${resultMap.prevCommandSet[0].class.name}"
	        resultMap.prevCommandSet == ["EHLO"] as Set
	}
	
	def "test handling HELO"() {
	    def ehloCommand = new EHLOCommand()
	    
	    def domain = "hot-groovy.com"
	    when:
	        def resultMap    = ehloCommand.process( "HELO ${domain}", [] as Set, [:] )
	        def ehloResponse = resultMap.resultString + crlf
	    then:
	        ehloResponse == "250 Hello ${domain}\r\n"
	}
	
	def "test handling domain more than 255 char"() {
	    def ehloCommand = new EHLOCommand()
	    def longString  = ( "f" * 252 ) + '.com'
	    when:
	        def resultMap    = ehloCommand.process( "EHLO ${longString}", [] as Set, [:] )
	        def ehloResponse = resultMap.resultString + crlf 
	    then:
	        ehloResponse == "501 Domain name length beyond 255 char limit per RFC 3696\r\n"
	}
	
	def "test that command list and buffer map are cleared"() {
	    def domain      = "www.groovymail.org"
	    def ehloCommand = new EHLOCommand()
	    
	    when:
	        def prevCommandSet = [ 'Get the hell off my ship', 'keep us under the radar, Wash' ] as Set
	        def bufferMap      = [ name:'Jayne', location: 'bunk' ]
	    then:
	        prevCommandSet.size() == 2
	        bufferMap.size()      == 2
	    when:
	        def resultMap    = ehloCommand.process( "EHLO ${domain}", prevCommandSet, bufferMap )
	        def ehloResponse = resultMap.resultString + crlf 
	        def newList      = resultMap.prevCommandSet
	        def newMap       = resultMap.bufferMap
	    then:
	        ehloResponse == "250-Hello ${domain}\r\n" +
	        "250-8BITMIME\r\n"   + 
	        "250-AUTH PLAIN\r\n" + 
	        "250 HELP\r\n"
	        newList.size() == 1
	        newList[ 0 ]   == "EHLO"
	        newMap.size()  == 0
	}

	def "test 501 for bad domain"() {
	    def domain      = "groovymail"
	    def ehloCommand = new EHLOCommand()
	    
	    when:
	        def prevCommandSet = [ 'Get the hell off my ship', 'keep us under the radar, Wash' ] as Set
	        def bufferMap      = [ name:'Jayne', location: 'bunk' ]
	    then:
	        prevCommandSet.size() == 2
	        bufferMap.size()      == 2
	    when:
	        def resultMap    = ehloCommand.process( "EHLO ${domain}", prevCommandSet, bufferMap )
	        def ehloResponse = resultMap.resultString + crlf 
	        def newList      = resultMap.prevCommandSet
	        def newMap       = resultMap.bufferMap
	    then:
	        ehloResponse   == "501 Syntax error in parameters or arguments\r\n" 
	        newList.size() == 1
	        newList[ 0 ]   == "EHLO"
	        newMap.size()  == 0
	}

    @Unroll( "test for valid domains returning #result for #testDomain" )
    def "test for valid domains returning #result for #testDomain"() {
       def ehloCommand = new EHLOCommand()
        when:
            def result = ehloCommand.isDomainValid( testDomain )
        then:
            println "domain was ${testDomain}, result is ${result}"
            result == value
            
        where:
            testDomain                              | value
            'sonic309-13.consmr.mail.bf2.yahoo.com' | true
            'o1.sendgrid.meetup.com'                | true
            'spring-chicken-as.twitter.com'         | true
            'some.domain'                           | true
            'mail.some.domain'                      | true
            'win-f0ciet00a57.domain'                | true
            'USER'                                  | false
            'ylmf-pc'                               | false // I see what you did there: https://serverfault.com/questions/667322/email-server-attack-from-telnet
            'zg-1222a-16'                           | false
            '533'                                   | false
            '[1.2.3.4]'                             | true // allowed under RFC 5321 4.1.3

    } // def "test for valid domains"

	def "test getting address"() {
	    def serverName  = "www.groovymail.org"
	    def ehloCommand = new EHLOCommand()
	    def inetAddress = Stub( InetAddress )
	    def address     = GroovyStub( Address )
	    address.getByName(_) >> inetAddress
	    inetAddress.hostAddress >> 'X.X.X.X'
	    def result = ehloCommand.processDomain( "XYZ" )
	    println "here is result: ${result}"
	    expect:
	        result == 'X.X.X.X'
	        1 == 1
	}
	
	@Ignore
	def "test without mocks or stubs"() {
	    when:
            def domain = "www.shelfunit.info"
            def ehloCommand = new EHLOCommand()
            def result = ehloCommand.processDomain( domain )
            println "here is result: ${result}"
	    then:
	        result == '45.33.18.182'
	}
	
	// this works even if not connected to internet
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
}

