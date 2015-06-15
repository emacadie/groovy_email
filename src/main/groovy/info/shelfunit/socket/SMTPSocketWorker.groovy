package info.shelfunit.socket

import java.io.InputStream
import java.io.OutputStream
import java.io.BufferedReader

import groovy.util.logging.Slf4j 

@Slf4j
class SMTPSocketWorker {

    private InputStream input
    private OutputStream output
	private String domain
    private String theResponse
    private String serverName
	private String prevCommand

	SMTPSocketWorker( argIn, argOut, argServerName ) {
        input = argIn
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
        def responseString
        while ( !gotQuitCommand ) {
	        holdString.clear()
	        responseString = ''
	        log.info "About to read input in the loop, gotQuitCommand: ${gotQuitCommand}"
	        // reader = input.newReader()
	        log.info "Got the reader, and it's a ${reader.getClass().getName()}"
	        def newString =  reader.readLine() 
	        log.info "Here is newString: ${newString}"
	        
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
		        output << responseString
	        } else if ( prevCommand == 'DATA' ) {
		        def sBuffer = new StringBuffer()
		        sBuffer << newString
		        while ( !newString.startsWith( "." ) ) {
			        
			        try {
				        newString = reader?.readLine()
				        sBuffer << newString
				        sBuffer << '\n'
				        log.info "in DATA loop, available: ${input.available()}, reader?.ready: ${reader?.ready()}"
			        } catch ( Exception ex ) {
				        log.info "exception: ${ex.printMessage()}"
				        ex.printStackTrace()
			        }
			        
		        }
		        responseString = this.handleMessage( sBuffer.toString() )
		        prevCommand = 'THE MESSAGE'
		        log.info "responseString after message: ${responseString}"
		        output << responseString
		        
	        } else {
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
			theResponse = "250-Hello ${domain}\n"
			theResponse += "250 HELP"
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
		}
		theResponse + "\r\n"
	}
}


