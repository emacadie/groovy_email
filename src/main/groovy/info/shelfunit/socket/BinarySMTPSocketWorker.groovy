package info.shelfunit.socket

import java.io.InputStream
import java.io.OutputStream
import java.io.BufferedReader
import groovy.util.logging.Slf4j 

@Slf4j
class BinarySMTPSocketWorker {

    private InputStream input
    private OutputStream output
	private String domain
    private String theResponse
    private String serverName
	private String prevCommand

	BinarySMTPSocketWorker( argIn, argOut, argServerName ) {
        input  = argIn
        output = argOut
        serverName = argServerName
        log.info "server name is ${serverName}"
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
        log.info "beginning doWork, input is a ${input.class.name}"
        log.info "name of current thread: ${Thread.currentThread().getName()}, and it is number ${Thread.currentThread().getId()}"            
        log.info "available: ${input.available()}"
        def reader = input.newReader()
        log.info "reader is a ${reader.class.name}"
        log.info "can reader still be read before output? ${reader.ready()}"
        def now = new Date()
        output << "220 ${serverName} Simple Mail Transfer Service Ready\r\n"
        
        log.info "can reader still be read after output? ${reader.ready()}"
        
        def holdString = new StringBuffer()
        def sBuff = new StringBuffer()
        def responseString
        def delimiter = '\r\n'
        def newString = ""
        def theByte
        def byteList = []
        def buffer 

        
        while ( !gotQuitCommand ) {
	        holdString.clear()
	        byteList.clear()
	        sBuff.clear()
	        responseString = ''
	        log.info "About to read input in the loop, gotQuitCommand: ${gotQuitCommand}, prevCommand: ${prevCommand}, delimiter: ${delimiter}"
	        //  reader = input.newReader()
	        // log.info "Got the reader, and it's a ${reader.getClass().getName()}"
	        // def newString =  reader.readLine() 
	        // log.info "Here is newString: ${newString}"
	        
	        while ( ( !sBuff.endsWith( delimiter ) ) && ( !sBuff.startsWith( 'RSET' ) ) ) {
	            theByte = input.read()
	            // log.info "theByte as char: ${theByte as char}"
	            if ( theByte == -1 ) {
	                break
	            }
	            byteList << theByte
	            sBuff << ( theByte as char )
	            // if ( sBuff.length() > 20 ) { log.info "sBuff bigger than 20: ${sBuff}" }
	            // log.info "sBuff bigger than 20: ${sBuff}"
            }
	        newString = sBuff.toString()
	        log.info "after the read, here is newString: ${newString}"
	        if ( newString.startsWith( 'QUIT' ) ) {
		        log.info "got QUIT, here is prevCommand: ${prevCommand}, here is newString: ${newString}"
		        responseString = this.handleMessage( newString )
		        log.info "responseString: ${responseString}"
		        output << responseString
		        gotQuitCommand = true
	        } else if ( newString.startsWith( 'DATA' ) ) {
		        responseString = this.handleMessage( newString )
		        log.info "responseString: ${responseString}"
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
				        log.info "exception: ${ex.printMessage()}"
				        ex.printStackTrace()
			        }
		        }
		        */
		        responseString = this.handleMessage( newString )
		        prevCommand = 'THE MESSAGE'
		        delimiter = "\r\n"
		        log.info "responseString after message: ${responseString}"
		        output << responseString
		        
	        } else {
	            delimiter = "\r\n"
		        responseString = this.handleMessage( newString )
		        log.info "responseString: ${responseString}"
		        output << responseString
	        }
        }
        log.info "ending doWork"
	}

	def handleMessage( theMessage ) {
		theResponse = ""
		log.info "Incoming message: ${theMessage}"
		if ( theMessage.startsWith( 'EHLO' ) ) {
			domain = theMessage.replaceFirst( 'EHLO ', '' )
			log.info "Here is the domain: ${domain}"
			theResponse = "250-Hello ${domain}\n250 HELP"
			// log.info "Here is the response:\n${theResponse}"
		} else if ( theMessage.startsWith( 'HELO' ) ) {
			domain = theMessage.replaceFirst( 'HELO ', '' )
			log.info "Here is the domain: ${domain}"
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
			log.info "prevCommand is DATA, here is the message: ${theMessage}"
			theResponse = '250 OK'
		} else if ( theMessage.startsWith( 'RSET' ) ) {
			theResponse = "250 OK"
		} else if ( prevCommand == 'THE MESSAGE' && theMessage.startsWith( 'QUIT' ) ) {
			theResponse = "221 ${serverName} Service closing transmission channel"
		} else {
			// log.info "prevCommand is DATA, here is the message: ${theMessage}"
			theResponse = '250 OK'
		}
		theResponse + "\r\n"
	}
}


