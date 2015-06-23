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
	// private String prevCommand
	private def prevCommandList 

	SMTPSocketWorker( argIn, argOut, argServerName ) {
        input = argIn
        output = argOut
        serverName = argServerName
        log.info "server name is ${serverName}"
        prevCommandList = []
	}
    
	// make sure private fields are truly private
	def setInput( arg ) {}
	def setOutput( arg ) {}
	def setDomain( arg ) {}
	def setTheResponse( arg ) {}
	// def setPrevCommand( arg ) {}
	def setPrevCommandList( arg ) {}

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
	        log.info "Got the reader, and it's a ${reader.getClass().getName()}"
	        def newString =  reader.readLine() 
	        log.info "Here is newString: ${newString}"
	        
	        if ( newString.startsWith( 'QUIT' ) ) {
		        log.info "got QUIT, here is prevCommandList: ${prevCommandList}, here is newString: ${newString}"
		        responseString = this.handleMessage( newString )
		        gotQuitCommand = true
		        log.info "Processed QUIT, here is gotQuitCommand: ${gotQuitCommand}"
	        } else if ( newString.startsWith( 'RSET' ) ) {
		        responseString = this.handleMessage( newString )
		    } else if ( newString.startsWith( 'DATA' ) ) {
		        responseString = this.handleMessage( newString )
		        prevCommandList << 'DATA'
	        } else if ( prevCommandList.lastItem() == 'DATA' ) {
		        def sBuffer = new StringBuffer()
		        sBuffer << newString
		        def readerReady = true
		        while ( readerReady ) { 
		        // while ( !newString.startsWith( "." ) ) {
		        
			        try {
				        newString = reader?.readLine()
				        sBuffer << newString << '\n'
				        readerReady = reader?.ready()
				        log.info "in DATA loop, available: ${input.available()}, reader?.ready: ${readerReady}"
				        log.info "Here is sBuffer in while loop: ${sBuffer}"
			        } catch ( Exception ex ) {
				        log.info "exception: ${ex.printMessage()}"
				        ex.printStackTrace()
			        }
		        }
		        log.info( "broke out of while loop for DATA" )
		        responseString = this.handleMessage( sBuffer.toString() )
		        prevCommandList << 'THE MESSAGE'
	        } else {
		        responseString = this.handleMessage( newString )
	        }
	        log.info "responseString: ${responseString}"
	        output << responseString
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
		} else if ( prevCommandList.lastItem() == 'DATA' ) {
			log.info "prevCommand is DATA, here is the message: ${theMessage}"
			theResponse = '250 OK'
		} else if ( theMessage.startsWith( 'RSET' ) ) {
		    prevCommandList.clear()
			theResponse = "250 OK"
		} else if ( prevCommandList.lastItem() == 'THE MESSAGE' && theMessage.startsWith( 'QUIT' ) ) {
			theResponse = "221 ${serverName} Service closing transmission channel"
		} else if ( theMessage.substring( 0, 4 ).matches( "SAML|SEND|SOML|TURN" ) ) {
		    theResponse = '502 Command not implemented'
		} else {
			// log.info "prevCommand is DATA, here is the message: ${theMessage}"
			// this should probably not stay 250
			theResponse = '250 OK'
		}
		theResponse + "\r\n"
	}
}


