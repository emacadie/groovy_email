package info.shelfunit.spool

import groovy.util.logging.Slf4j 
import info.shelfunit.mail.ConfigHolder
import java.io.IOException
import java.sql.SQLException

@Slf4j
class MessageSender {
    
    MessageSender() {
        log.info "Starting new MessageSender"
    }
    
    def doWork( input, output, row, otherDomain, otherUserList, outboundDomain  ) {
        log.info "In doWork"
        def areWeDone = false
        def inputLine
        def reader = input.newReader()
        log.info "About to read input line"
        def outString
        def newString =  reader.readLine() 
        log.info "Here is newString: ${newString}"
        log.info "Here is row: it's a ${row.getClass().name}"
        log.info "About to send: EHLO ${outboundDomain}"
        output << "EHLO ${outboundDomain}\r\n"
        def doneWith220 = false
        while ( isNot( doneWith220 ) ) {
            newString = reader.readLine()
            if ( doesNot( newString.matches( ".*[a-z].*" ) ) ) {
                commandList << newString.allButFirstFour()
            }
            log.info "Here is newString: ${newString}"
            if ( newString.startsWith( '250 ' ) ) { 
                doneWith220 = true 
            }
            log.info "doneWith220: ${doneWith220}"
        }
        log.info "Here is commandList: ${commandList}"
        log.info "About to send MAIL FROM:<${row.from_address}>"
        output << "MAIL FROM:<${row.from_address}>\r\n"
        newString = reader.readLine()
        log.info "Got response ${newString}"
        def got250ForRCPT = false
        otherUserList.each { uName ->
            log.info "About to send RCPT TO:<${uName}>"
            output << "RCPT TO:<${uName}>\r\n"
            newString = reader.readLine()
            log.info "Got response ${newString}"
            if ( newString.startsWith( "250" ) ) {
                got250ForRCPT = true
            }
        }
        if ( isNot( got250ForRCPT ) ) {
            output << "QUIT\r\n"
        } else {
            output << "DATA\r\n"
            newString = reader.readLine()
            log.info "Here is response to DATA: ${newString}"
            if ( newString.startWith( "354" ) ) {
                output << row[ text_body ]
                output << ".\r\n"
            }
            newString = reader.readLine()
            log.info "Here is newLine: ${newLine}"
            output << "QUIT\r\n"
        }
        newString = reader.readLine()
        log.info "Here is newLine after QUIT: ${newLine}"
        
        // log.info "Here is inputLine: ${inputLine}"
        /*
        while ( !areWeDone ) {
        }
        */
    }
}

