package info.shelfunit.postoffice

import java.io.InputStream
import java.io.OutputStream

import groovy.sql.Sql
import groovy.util.logging.Slf4j 

import info.shelfunit.mail.ConfigHolder
import info.shelfunit.postoffice.command.DELECommand
import info.shelfunit.postoffice.command.LISTCommand
import info.shelfunit.postoffice.command.PASSCommand
import info.shelfunit.postoffice.command.QUITCommand
import info.shelfunit.postoffice.command.RETRCommand
import info.shelfunit.postoffice.command.RSETCommand
import info.shelfunit.postoffice.command.STATCommand
import info.shelfunit.postoffice.command.USERCommand

import visibility.Hidden

@Slf4j
class ModularPostOfficeSocketWorker {

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
	
	@Hidden def deleCommand
	@Hidden def listCommand
	@Hidden def quitCommand
	@Hidden def passCommand
	@Hidden def rsetCommand
	@Hidden def retrCommand
	@Hidden def statCommand
	@Hidden def userCommand
	
	@Hidden def commandResultMap

	ModularPostOfficeSocketWorker( argIn, argOut, argServerList ) {
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
        
        userCommand = new USERCommand( sql )
        passCommand = new PASSCommand( sql )
        statCommand = new STATCommand( sql )
        listCommand = new LISTCommand( sql )
        deleCommand = new DELECommand( sql )
        quitCommand = new QUITCommand( sql, serverName )
        retrCommand = new RETRCommand( sql )
        rsetCommand = new RSETCommand( sql )
	}

	def doWork() {
        String sCurrentLine
        def gotQuitCommand = false
        log.info "beginning doWork, input is a ${input.class.name}"
        log.info "name of current thread: ${Thread.currentThread().getName()}, and it is number ${Thread.currentThread().getId()}"            
        log.info "available: ${input.available()}"
        def reader = input.newReader()
        log.info "reader is a ${reader.class.name}"
        output << "+OK POP3 server ready <${serverName}>\r\n"
        bufferMap.state = 'AUTHORIZATION'
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
		} else if ( theMessage.startsWith( 'QUIT' ) ) { 
			theResponse = "221 ${serverName} Service closing transmission channel"
		} else if ( theMessage.startsWith( 'NOOP' ) ) { // This is in POP3
		    theResponse = '+OK'
		} else if ( theMessage.isObsoleteCommand() ) { 
		    theResponse = '502 Command not implemented'
		} else {
			theResponse = '+OK'
		}
		theResponse + "\r\n"
	}
	
	def returnCurrentCommand( theMessage, isActualMessage ) {
	    log.info "in returnCurrentCommand, here is value of isActualMessage: ${isActualMessage}"
		if ( isActualMessage ) {
		    return mssgCommand
		} else if ( theMessage.startsWith( 'USER' ) ) {
			return userCommand
		} else if ( theMessage.startsWith( 'PASS' ) ) {
			return passCommand
		} else if ( theMessage.startsWith( 'RSET' ) ) {
		    return rsetCommand
		} else if ( theMessage.startsWith( 'STAT' ) ) {
		    return statCommand
		} else if ( theMessage.startsWith( 'RETR' ) ) {
		    return retrCommand
		} else if ( theMessage.startsWith( 'LIST' ) ) {
		    return listCommand
		} else if ( theMessage.startsWith( 'QUIT' ) ) {
		    return quitCommand
		} else if ( theMessage.startsWith( 'DELE' ) ) {
		    return deleCommand
		}
	}
}

