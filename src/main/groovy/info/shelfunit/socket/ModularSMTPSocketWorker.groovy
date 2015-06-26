package info.shelfunit.socket

import java.io.InputStream
import java.io.OutputStream
import java.io.BufferedReader

import groovy.util.logging.Slf4j 

import info.shelfunit.socket.command.EHLOCommand
import info.shelfunit.socket.command.MAILCommand

@Slf4j
class ModularSMTPSocketWorker {

    private InputStream input
    private OutputStream output
	private String domain
    private String theResponse
    private String serverName
	private prevCommandList 
	private mailCommand
	private ehloCommand
	private commandResultMap

	ModularSMTPSocketWorker( argIn, argOut, argServerName ) {
        input = argIn
        output = argOut
        serverName = argServerName
        log.info "server name is ${serverName}"
        prevCommandList = []
        commandResultMap = [:]
        mailCommand = new MAILCommand()
        ehloCommand = new EHLOCommand()
	}
    
	// make sure private fields are truly private
	def setInput( arg ) {}
	def setOutput( arg ) {}
	def setDomain( arg ) {}
	def setTheResponse( arg ) {}
	def setPrevCommandList( arg ) {}

	def doWork() {
        String sCurrentLine
        def gotQuitCommand = false
        log.info "beginning doWork, input is a ${input.class.name}"
        log.info "name of current thread: ${Thread.currentThread().getName()}, and it is number ${Thread.currentThread().getId()}"            
        log.info "available: ${input.available()}"
        def reader = input.newReader()
        log.info "reader is a ${reader.class.name}"
        output << "220 ${serverName} Simple Mail Transfer Service Ready\r\n"

        def responseString
        while ( !gotQuitCommand ) {
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
		        while ( !newString.equals( "." ) ) {
		            sBuffer << newString << '\n'
			        try {
				        newString = reader?.readLine()
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
		if ( theMessage.startsWith( 'EHLO' ) || theMessage.startsWith( 'HELO' )  ) {
		    commandResultMap.clear()
		    commandResultMap = ehloCommand.process( theMessage, prevCommandList ) 
		    prevCommandList = commandResultMap.prevCommandList.clone()
			theResponse = commandResultMap.resultString
		} else if ( theMessage.startsWith( 'MAIL' ) ) {
			// temporary
			// commandResultMap = mailCommand.process( theMessage, prevCommandList )
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
		} else if ( theMessage.startsWith( 'QUIT' ) ) { // prevCommandList.lastItem() == 'THE MESSAGE' && 
			theResponse = "221 ${serverName} Service closing transmission channel"
		} else if ( theMessage.substring( 0, 4 ).matches( "SAML|SEND|SOML|TURN" ) ) {
		    theResponse = '502 Command not implemented'
		} else if ( theMessage.startsWith( 'EXPN' ) ) {
		    theResponse = '502 Command not implemented'
		} else if ( theMessage.startsWith( 'NOOP' ) ) {
		    theReponse = '250 OK'
		} else {
			// log.info "prevCommand is DATA, here is the message: ${theMessage}"
			// this should probably not stay 250
			theResponse = '250 OK'
		}
		theResponse + "\r\n"
	}
}


