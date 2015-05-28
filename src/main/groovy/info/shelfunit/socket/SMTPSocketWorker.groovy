package info.shelfunit.socket

// good version: 
// https://github.com/emacadie/groovy_email/blob/cd2a0d27d88e7eee54c9315ce7d6e6b52808840c/src/main/groovy/info/shelfunit/socket/SMTPSocketWorker.groovy

import java.io.InputStream
import java.io.OutputStream
import java.io.BufferedReader

class SMTPSocketWorker {

    private InputStream input
    private OutputStream output
	private String domain
    private String theResponse
    private String serverName

    SMTPSocketWorker( argIn, argOut, argServerName ) {
        input = argIn
        output = argOut
        serverName = argServerName
        println "server name is ${serverName}"
    }
    
	// make sure private fields are truly private
	def setInput( arg ) {}
	def setOutput( arg ) {}
	def setDomain( arg ) {}
	def setTheResponse( arg ) {}

    def doWork() {
        String sCurrentLine
        def gotQuitCommand = false
        println "beginning doWork, input is a ${input.class.name}"
        println "name of current thread: ${Thread.currentThread().getName()}, and it is number ${Thread.currentThread().getId()}"            
        println "available: ${input.available()}"
        def reader = input.newReader()
        println "reader is a ${reader.class.name}"
        // def buffer = reader.readLine()
        /*
	

        */
        // println "server received: $buffer"
        println "can reader still be read before output? ${reader.ready()}"
        def now = new Date()
        output << "220 ${serverName} Simple Mail Transfer Service Ready\r\n"
        
        println "can reader still be read after output? ${reader.ready()}"
        
        
        def responseString
        while ( !gotQuitCommand ) {
	        // holdString.delete( 0, holdString.length() )
	        def holdString = new StringBuffer()
	        responseString = ''
	        // reader = input.newReader()
	        println "About to read input in the loop"
	        // def newString =  input.newReader().getText() 
	        // def newString =  input.newReader().readLine() 
	        // def lineList = input.newReader().readLines() 
	        // println "Here is lineList: ${lineList}"
	        // def newString = lineList.join()
	        def lineList = []
	        def readerA = input.newReader()
	        while ( ( sCurrentLine = readerA.readLine() ) != null ) {
		        println "sCurrentLine: ${sCurrentLine}"
		        lineList << sCurrentLine
	        }

	        def newString = new String()
	        println "Here is lineList: ${lineList}"
	        
	        newString = lineList.join()
	        /*
	        reader.eachLine { theLine ->
		        // newString = newString + theLine
		        holdString.append( theLine )
		        // println "Here is theLine: ${theLine}"
		        
	        }
	        */
	        println "Here is newString: ${newString}"
	        
	        if ( newString.startsWith( 'QUIT' ) ) {
		        gotQuitCommand = true
	        } else {
		        responseString = this.handleMessage( newString )
		        println "responseString: ${responseString}"
		        output << responseString
	        }
        }
        
        /*
        def buffer = reader.readLine()
        println "Here is the buffer: ${buffer}\nand it's a ${buffer.getClass().getName()}"
        theResponse = this.handleMessage( buffer )
        println "theResponse is a ${theResponse.class.name}"
        output << theResponse
        println "sent response"
        buffer = input.newReader().readLine()
        println "buffer after responding to ELHO: ${buffer}\nand it's a ${buffer.getClass().getName()}"
        */
        println "ending doWork"
    }

	def handleMessage( theMessage ) {
		theResponse = ""
		print "Incoming message: ${theMessage}"
		if ( theMessage.startsWith( 'EHLO' ) ) {
			domain = theMessage.replaceFirst( 'EHLO ', '' )
			println "Here is the domain: ${domain}"
			theResponse = "250-Hello ${domain}\n"
		    theResponse += "250 HELP\r\n"
			println "Here is the response:\n${theResponse}"
		} else if ( theMessage.startsWith( 'HELO' ) ) {
			domain = theMessage.replaceFirst( 'HELO ', '' )
			println "Here is the domain: ${domain}"
			theResponse = "250 Hello ${domain}\r\n"
		} else if ( theMessage.startsWith( 'MAIL' ) ) {
			// temporary
			theResponse = "250 OK\r\n"
		} else if ( theMessage.startsWith( 'RCPT' ) ) {
			// temporary
			theResponse = "250 OK\r\n"
		} else if ( theMessage.startsWith( 'DATA' ) ) {
			theResponse = "354 Start mail input; end with <CRLF>.<CRLF>\r\n"
		}
		theResponse
	}
}

