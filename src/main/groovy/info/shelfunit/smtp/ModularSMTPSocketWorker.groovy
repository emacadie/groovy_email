package info.shelfunit.smtp

import java.io.InputStream
import java.io.OutputStream

import groovy.sql.Sql
import groovy.util.logging.Slf4j 

import info.shelfunit.mail.ConfigHolder
import info.shelfunit.smtp.command.AUTHCommand
import info.shelfunit.smtp.command.DATACommand
import info.shelfunit.smtp.command.EHLOCommand
import info.shelfunit.smtp.command.MAILCommand
import info.shelfunit.smtp.command.MSSGCommand
import info.shelfunit.smtp.command.RCPTCommand
import info.shelfunit.smtp.command.QUITCommand
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
	@Hidden def domainList
	@Hidden def commandResultMap

	ModularSMTPSocketWorker( argIn, argOut, argDomainList ) {
        input = argIn
        output = argOut
        
        def db = ConfigHolder.instance.returnDbMap()         
        sql = Sql.newInstance( db.url, db.user, db.password, db.driver )
        domainList = []
        argDomainList.collect{ domainList << it.toLowerCase() }
        
        serverName = domainList[ 0 ]
        log.info "server name is ${serverName}"
        prevCommandSet = [] as Set
        commandResultMap = [:]
        bufferMap = [:]
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
        while ( doNot( gotQuitCommand ) ) {
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

	        } else if ( prevCommandSet?.lastItem() == 'DATA' ) {
		        def sBuffer = new StringBuffer()
		        while ( doesNot( newString.equals( "." ) ) ) {
		            sBuffer << newString 
		            sBuffer << "\r\n"
			        try {
				        newString = reader?.readLine()
				        // log.info "Here is sBuffer in while loop: ${sBuffer}"
			        } catch ( Exception ex ) {
				        log.info "exception: ${ex.printMessage()}"
				        ex.printStackTrace()
			        }
		        }
		        // sBuffer << newString << "\n"
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
	
	def cleanup() {
	    sql.close()
	}

	def handleMessage( theMessage, def isActualMessage = false ) {
		theResponse = ""
		log.info "Incoming message: ${theMessage}"
		if ( theMessage.isEncapsulated( ) || isActualMessage ) {
		    def commandObject = this.returnCurrentCommand( theMessage.firstTen(), isActualMessage )
		    log.info "returned a command object that is a ${commandObject.class.name}"
		    commandResultMap.clear()
		    commandResultMap = commandObject.process( theMessage, prevCommandSet, bufferMap ) 
		    prevCommandSet = commandResultMap.prevCommandSet.clone()
		    bufferMap = commandResultMap.bufferMap?.clone() 
			theResponse = commandResultMap.resultString
		} else if ( prevCommandSet.lastItem() == 'DATA' ) {
			log.info "prevCommand is DATA, here is the message: ${theMessage}"
			theResponse = '250 OK'
		} else if ( theMessage.startsWith( 'EXPN' ) ) {
		    theResponse = '502 Command not implemented'
		} else if ( theMessage.startsWith( 'NOOP' ) ) {
		    theResponse = '250 OK'
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
		    return new MSSGCommand( sql, domainList )
		} else if ( theMessage.isHelloCommand() ) {
		    return new EHLOCommand()
		} else if ( theMessage.startsWith( 'MAIL' ) ) {
			return new MAILCommand( sql, domainList )
		} else if ( theMessage.startsWith( 'RCPT' ) ) {
			return new RCPTCommand( domainList )
		} else if ( theMessage.startsWith( 'RSET' ) ) {
		    return new RSETCommand()
		} else if ( theMessage.startsWith( 'DATA' ) ) {
		    return new DATACommand()
		} else if ( theMessage.startsWith( 'QUIT' ) ) {
		    return new QUITCommand( domainList )
		} else if ( theMessage.startsWith( 'AUTH' ) ) {
		    return new AUTHCommand( sql, domainList )
		}
	}

} // line 166


