package info.shelfunit.smtp

import java.io.InputStream
import java.io.OutputStream
import java.sql.SQLException
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
    @Hidden rawCommandList
    @Hidden def bufferMap
    @Hidden def copyMap
    @Hidden def sql
    @Hidden def domainList
    @Hidden def commandResultMap
    @Hidden def mssgUUID
    @Hidden def fromIPAddress
    @Hidden def fromHostName
    @Hidden def fromUserName
    @Hidden def toAddressListString
    @Hidden def statusString

    ModularSMTPSocketWorker( argIn, argOut, argDomainList, argFromAddress, argFromHost ) {
        input  = argIn
        output = argOut
        
        def db     = ConfigHolder.instance.returnDbMap()         
        sql        = Sql.newInstance( db.url, db.user, db.password, db.driver )
        domainList = []
        argDomainList.collect{ domainList << it.toLowerCase() }
        
        serverName = domainList[ 0 ]
        log.info "server name is ${serverName}"
        statusString     = "ABORTED BY THEM"
        prevCommandSet   = [] as Set
        rawCommandList   = []
        commandResultMap = [:]
        bufferMap        = [:]
        copyMap          = [:]
        // bufferMap.rawCommandList = rawCommandList
        bufferMap.fromIPAddress  = argFromAddress
        bufferMap.fromHostName = argFromHost
        fromIPAddress = argFromAddress
        fromHostName  = argFromHost
        mssgUUID               = UUID.randomUUID()
        bufferMap.mssgUUID     = mssgUUID
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
        while ( _not( gotQuitCommand ) ) {
            responseString = ''
            log.info "About to read input in the loop, gotQuitCommand: ${gotQuitCommand}"
            log.info "Got the reader, and it's a ${reader.getClass().getName()}"
            def newString =  reader.readLine() 
            log.info "Here is newString: ${newString}"
            
            if ( newString.startsWith( 'QUIT' ) ) {
                // QUITCommand clears the map, so I might want to call a method here to record the status
                log.info "got QUIT, here is prevCommandSet: ${prevCommandSet}, here is newString: ${newString}"
                responseString = this.handleMessage( newString )
                gotQuitCommand = true
                log.info "Processed QUIT, here is gotQuitCommand: ${gotQuitCommand}"
                rawCommandList << newString
            } else if ( prevCommandSet?.lastItem() == 'DATA' ) {
                def sBuffer = new StringBuilder()
                while ( _not( newString == "."  ) ) {
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
                rawCommandList << newString
            }
            log.info "responseString: ${responseString}"
            output << responseString
        }
        log.info "Here is prevCommandSet: ${prevCommandSet}"
        log.info "here is rawCommandList: ${rawCommandList}"
        log.info "here is rawCommandList.length: ${rawCommandList.size}"
        log.info "Here is bufferMap: ${bufferMap}"
        log.info "Here is copyMap: ${copyMap}"
        this.commitIncomingMailLog()
        // do sql stuff here
        // 
        log.info "ending doWork"
    } 

    def commitIncomingMailLog(  ) {
      def sqlString = 'insert into mail_from_log( id, from_ip_address, from_username, from_domain, to_address_list, status_string, command_sequence ) values (?, ?, ?, ?, ?, ?, ?)'
      try {
            sql.withTransaction {
                log.info "About to call sql to enter message"
                def insertCounts = sql.withBatch( sqlString ) { stmt ->
                    log.info "stmt is a ${stmt.class.name}"
                    stmt.addBatch( [ 
                        UUID.randomUUID(), // id, 
                        fromIPAddress,  // from_address, 
                        fromUserName,   // from_username, 
                        fromHostName, // from_domain, 
                        toAddressListString, // to_address_list, 
                        statusString,  // status_string // I might need to change this
                        rawCommandList.toString() //command_sequence
                        
                    ] )
                }
            }
        } catch ( Exception e ) {
            log.info "Next exception message: ${e.getMessage()}"
            log.error "something went wrong", e

            SQLException ex = e.getNextException()
            // log.info "Next exception message: ${ex?.getMessage()}"
            // log.error "something went wrong", ex? 

        }

    } // def commitIncomingMailLog()
    
    def cleanup() {
        sql.close()
    }

    def updateVarsFromBufferMap() {
        if ( this.bufferMap.reversePath?.length() > 0 ) {
            this.fromUserName = this.bufferMap.reversePath
        }
        if ( this.bufferMap.forwardPath?.size() > 0 ) {
            this.toAddressListString = this.bufferMap.forwardPath.join( "," )
        }
        if ( this.bufferMap.statusString?.length() > 0 ) {
            this.statusString = this.bufferMap.statusString
        }
    }
    
    def handleMessage( theMessage, def isActualMessage = false ) {
        theResponse = ""
        log.info "Incoming message: ${theMessage}"
        if ( theMessage.isEncapsulated( ) || isActualMessage ) {
            def commandObject = this.returnCurrentCommand( theMessage.firstTen(), isActualMessage )
            log.info "returned a command object that is a ${commandObject.class.name}"
            commandResultMap.clear()
            commandResultMap = commandObject.process( theMessage, prevCommandSet, bufferMap ) 
            prevCommandSet   = commandResultMap.prevCommandSet.clone()
            bufferMap        = commandResultMap.bufferMap?.clone()
            this.updateVarsFromBufferMap()
            theResponse      = commandResultMap.resultString
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
            return new MSSGCommand( this.mssgUUID, sql, domainList )
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


