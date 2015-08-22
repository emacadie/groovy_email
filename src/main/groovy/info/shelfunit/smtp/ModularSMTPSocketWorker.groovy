package info.shelfunit.smtp

import java.io.InputStream
import java.io.OutputStream

import groovy.sql.Sql
import groovy.util.logging.Slf4j 

import info.shelfunit.mail.ConfigHolder
import info.shelfunit.smtp.command.DATACommand
import info.shelfunit.smtp.command.EHLOCommand
import info.shelfunit.smtp.command.MAILCommand
import info.shelfunit.smtp.command.MSSGCommand
import info.shelfunit.smtp.command.RCPTCommand
import info.shelfunit.smtp.command.RSETCommand

import visibility.Hidden

@Slf4j
class ModularSMTPSocketWorker {

    @Hidden InputStream input
    @Hidden OutputStream output
	@Hidden String domain
    @Hidden String theResponse
    @Hidden String serverName
	@Hidden prevCommandSet 
	@Hidden def bufferMap
	@Hidden def sql
	@Hidden def serverList
	@Hidden def commandObject
	@Hidden def mailCommand
	@Hidden def ehloCommand
	@Hidden def rcptCommand
	@Hidden def rsetCommand
	@Hidden def dataCommand
	@Hidden def mssgCommand
	@Hidden def commandResultMap

	ModularSMTPSocketWorker( argIn, argOut, argServerList ) {
        input = argIn
        output = argOut
        
        def db = ConfigHolder.instance.returnDbMap()         
        sql = Sql.newInstance( db.url, db.user, db.password, db.driver )
        
        serverList = argServerList
        serverName = serverList[ 0 ]
        log.info "server name is ${serverName}"
        prevCommandSet = [] as Set
        commandResultMap = [:]
        bufferMap = [:]
        mailCommand = new MAILCommand()
        ehloCommand = new EHLOCommand()
        rcptCommand = new RCPTCommand( sql, serverList )
        rsetCommand = new RSETCommand()
        dataCommand = new DATACommand()
        mssgCommand = new MSSGCommand( sql, serverList )
	}

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
		        log.info "got QUIT, here is prevCommandSet: ${prevCommandSet}, here is newString: ${newString}"
		        responseString = this.handleMessage( newString )
		        gotQuitCommand = true
		        log.info "Processed QUIT, here is gotQuitCommand: ${gotQuitCommand}"

	        } else if ( prevCommandSet.lastItem() == 'DATA' ) {
		        def sBuffer = new StringBuffer()
		        while ( !newString.equals( "." ) ) {
		            sBuffer << newString << '\n'
			        try {
				        newString = reader?.readLine()
				        // log.info "Here is sBuffer in while loop: ${sBuffer}"
			        } catch ( Exception ex ) {
				        log.info "exception: ${ex.printMessage()}"
				        ex.printStackTrace()
			        }
		        }
		        log.info( "broke out of while loop for DATA" )
		        responseString = this.handleMessage( sBuffer.toString(), true )
		        prevCommandSet << 'THE MESSAGE'
	        } else {
		        responseString = this.handleMessage( newString )
	        }
	        log.info "responseString: ${responseString}"
	        output << responseString
        }
        log.info "Here is prevCommandSet: ${prevCommandSet}"
        log.info "ending doWork"
	} 

	def handleMessage( theMessage, def isActualMessage = false ) {
		theResponse = ""
		log.info "Incoming message: ${theMessage}"
		if ( theMessage.isEncapsulated( ) || isActualMessage ) {
		    commandObject = this.returnCurrentCommand( theMessage, isActualMessage )
		    log.info "returned a command object that is a ${commandObject.class.name}"
		    commandResultMap.clear()
		    commandResultMap = commandObject.process( theMessage, prevCommandSet, bufferMap ) 
		    prevCommandSet = commandResultMap.prevCommandSet.clone()
		    bufferMap = commandResultMap.bufferMap.clone() 
			theResponse = commandResultMap.resultString
		} else if ( prevCommandSet.lastItem() == 'DATA' ) {
			log.info "prevCommand is DATA, here is the message: ${theMessage}"
			theResponse = '250 OK'
		} else if ( theMessage.startsWith( 'QUIT' ) ) { // prevCommandSet.lastItem() == 'THE MESSAGE' && 
			theResponse = "221 ${serverName} Service closing transmission channel"
		} else if ( theMessage.startsWith( 'EXPN' ) ) {
		    theResponse = '502 Command not implemented'
		} else if ( theMessage.startsWith( 'NOOP' ) ) {
		    theReponse = '250 OK'
		} else if ( theMessage.startsWith( 'VRFY' ) ) {
		    "252 VRFY Disabled, returning argument ${theMesssage.allButFirstFour()}"
		} else if ( theMessage.isObsoleteCommand() ) { 
		    theResponse = '502 Command not implemented'
		} else {
			theResponse = '250 OK'
		}
		theResponse + "\r\n"
	}
	
	def returnCurrentCommand( theMessage, isActualMessage ) {
	    log.info "in returnCurrentCommand, here is value of isActualMessage: ${isActualMessage}"
		if ( isActualMessage ) {
		    return mssgCommand
		} else if ( theMessage.isHelloCommand() ) {
		    return ehloCommand
		} else if ( theMessage.startsWith( 'MAIL' ) ) {
			return mailCommand
		} else if ( theMessage.startsWith( 'RCPT' ) ) {
			return rcptCommand
		} else if ( theMessage.startsWith( 'RSET' ) ) {
		    return rsetCommand
		} else if ( theMessage.startsWith( 'DATA' ) ) {
		    return dataCommand
		}
	}
}


