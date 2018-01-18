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
    @Hidden def sqlObject
    @Hidden def serverList
    
    @Hidden def commandResultMap
    
    ModularPostOfficeSocketWorker( argIn, argOut, argServerList ) {
        input = argIn
        output = argOut
        
        def db = ConfigHolder.instance.returnDbMap()         
        sqlObject = Sql.newInstance( db.url, db.user, db.password, db.driver )
        log.info "argServerList is a ${argServerList.class.name}"
        serverList = []
        argServerList.collect{ serverList << it.toLowerCase() }
        serverName = serverList[ 0 ]
        log.info "server name is ${serverName}"
        prevCommandSet   = [] as Set
        commandResultMap = [:]
        bufferMap        = [:]
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
            
            if ( newString == null ) { 
                responseString = "+OK\r\n"
            } else if ( newString.startsWith( 'QUIT' ) ) {
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
    
    def cleanup () {
        sqlObject.close()
    }
    
    def handleMessage( theMessage, def isActualMessage = false ) {
        theResponse = ""
        log.info "Incoming message: ${theMessage}"
        if ( theMessage.isOptionalPostOfficeCommand() ) {
            theResponse = '-ERR Command not implemented'
        } else if ( theMessage.startsWith( 'NOOP' ) ) { // This is in POP3
            theResponse = '+OK'
        } else if ( theMessage.isRFC5034Command() ) { 
            theResponse = '-ERR Command not implemented'
        } else if ( theMessage.isEncapsulated( ) || isActualMessage ) {
            def commandObject = this.returnCurrentCommand( theMessage, isActualMessage )
            log.info "returned a command object that is a ${commandObject.class.name}"
            commandResultMap.clear()
            commandResultMap = commandObject.process( theMessage, prevCommandSet, bufferMap ) 
            prevCommandSet   = commandResultMap.prevCommandSet.clone()
            bufferMap        = commandResultMap.bufferMap.clone() 
            theResponse      = commandResultMap.resultString
        
        } else {
            theResponse = '-ERR Command not implemented'
        }
        theResponse + "\r\n"
    }
    
    def returnCurrentCommand( theMessage, isActualMessage ) {
        log.info "in returnCurrentCommand, here is value of isActualMessage: ${isActualMessage}"
        if ( isActualMessage ) {
            return mssgCommand
        } else if ( theMessage.startsWith( 'USER' ) ) {
            return new USERCommand( sqlObject )
        } else if ( theMessage.startsWith( 'PASS' ) ) {
            return new PASSCommand( sqlObject )
        } else if ( theMessage.startsWith( 'RSET' ) ) {
            return new RSETCommand( sqlObject )
        } else if ( theMessage.startsWith( 'STAT' ) ) {
            return new STATCommand( sqlObject )
        } else if ( theMessage.startsWith( 'RETR' ) ) {
            return new RETRCommand( sqlObject )
        } else if ( theMessage.startsWith( 'LIST' ) ) {
            return new LISTCommand( sqlObject )
        } else if ( theMessage.startsWith( 'QUIT' ) ) {
            return new QUITCommand( sqlObject, serverName )
        } else if ( theMessage.startsWith( 'DELE' ) ) {
            return new DELECommand( sqlObject )
        }
    }
} // line 156

