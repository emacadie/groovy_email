package info.shelfunit.socket

import java.io.InputStream
import java.io.OutputStream
import java.io.BufferedReader

// 

class BinarySMTPSocketWorker {

    private InputStream input
    private OutputStream output
	private String domain
    private String theResponse
    private String serverName
	private String prevCommand

	BinarySMTPSocketWorker( argIn, argOut, argServerName ) {
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
	def setPrevCommand( arg ) {}

	def doWork() {
        String sCurrentLine
        def gotQuitCommand = false
        println "beginning doWork, input is a ${input.class.name}"
        println "name of current thread: ${Thread.currentThread().getName()}, and it is number ${Thread.currentThread().getId()}"            
        println "available: ${input.available()}"
        def reader = input.newReader()
        println "reader is a ${reader.class.name}"
        println "can reader still be read before output? ${reader.ready()}"
        def now = new Date()
        output << "220 ${serverName} Simple Mail Transfer Service Ready\r\n"
        
        println "can reader still be read after output? ${reader.ready()}"
        
        def holdString = new StringBuffer()
        def sBuff = new StringBuffer()
        def responseString
        def delimiter = '\r\n'
        def newString = ""
        def theByte
        def holder
        def buffer 

        
        while ( !gotQuitCommand ) {
	        holdString.clear()
	        responseString = ''
	        println "About to read input in the loop, gotQuitCommand: ${gotQuitCommand}"
	        //  reader = input.newReader()
	        // println "Got the reader, and it's a ${reader.getClass().getName()}"
	        // def newString =  reader.readLine() 
	        // println "Here is newString: ${newString}"
	        sBuff.clear()
	        while ( ( !sBuff.endsWith( delimiter ) ) ) {
	            theByte = input.read()
	            // println "theByte as char: ${theByte as char}"
	            sBuff << ( theByte as char )
	            // if ( sBuff.length() > 20 ) { println "sBuff bigger than 20: ${sBuff}" }
            }
	        newString = sBuff.toString()
	        if ( newString.startsWith( 'QUIT' ) ) {
		        println "got QUIT, here is prevCommand: ${prevCommand}, here is newString: ${newString}"
		        responseString = this.handleMessage( newString )
		        println "responseString: ${responseString}"
		        output << responseString
		        gotQuitCommand = true
	        } else if ( newString.startsWith( 'DATA' ) ) {
		        responseString = this.handleMessage( newString )
		        println "responseString: ${responseString}"
		        prevCommand = 'DATA'
		        delimiter = "\r\n.\r\n"
		        output << responseString
	        } else if ( prevCommand == 'DATA' ) {
		        /*
	            def sBuffer = new StringBuffer()
		        sBuffer << newString
		        while ( !newString.startsWith( "." ) ) {
			        try {
				        newString = reader?.readLine()
				        sBuffer << newString
				        sBuffer << '\n'
			        } catch ( Exception ex ) {
				        println "exception: ${ex.printMessage()}"
				        ex.printStackTrace()
			        }
		        }
		        */
		        responseString = this.handleMessage( newString )
		        prevCommand = 'THE MESSAGE'
		        delimiter = "\r\n"
		        println "responseString after message: ${responseString}"
		        output << responseString
		        
	        } else {
		        responseString = this.handleMessage( newString )
		        println "responseString: ${responseString}"
		        output << responseString
	        }
        }
        
        println "ending doWork"
	}

	def handleMessage( theMessage ) {
		theResponse = ""
		print "Incoming message: ${theMessage}"
		if ( theMessage.startsWith( 'EHLO' ) ) {
			domain = theMessage.replaceFirst( 'EHLO ', '' )
			println "Here is the domain: ${domain}"
			theResponse = "250-Hello ${domain}\n"
			theResponse += "250 HELP"
			// println "Here is the response:\n${theResponse}"
		} else if ( theMessage.startsWith( 'HELO' ) ) {
			domain = theMessage.replaceFirst( 'HELO ', '' )
			println "Here is the domain: ${domain}"
			theResponse = "250 Hello ${domain}"
		} else if ( theMessage.startsWith( 'MAIL' ) ) {
			// temporary
			theResponse = "250 OK"
		} else if ( theMessage.startsWith( 'RCPT' ) ) {
			// temporary
			theResponse = "250 OK"
		} else if ( theMessage.startsWith( 'DATA' ) ) {
			theResponse = "354 Start mail input; end with <CRLF>.<CRLF>"
		} else if ( prevCommand == 'DATA' ) {
			// println "prevCommand is DATA, here is the message: ${theMessage}"
			theResponse = '250 OK'
		} else if ( prevCommand == 'THE MESSAGE' && theMessage.startsWith( 'QUIT' ) ) {
			theResponse = "221 ${serverName} Service closing transmission channel"
		}
		theResponse + "\r\n"
	}
}


