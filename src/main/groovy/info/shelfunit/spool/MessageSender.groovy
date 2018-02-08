package info.shelfunit.spool

import groovy.util.logging.Slf4j 

@Slf4j
class MessageSender {
    
    MessageSender() {
        log.info "Starting new MessageSender"
    }
    
    def doWork( input, output, messageRow, otherDomain, otherUserList, outboundDomain ) {
        log.info "In doWork"
        def areWeDone = false
        def inputLine
        def reader = input.newReader()
        log.info "About to read input line"
        def outString
        def newString =  reader.readLine() 
        log.info "Here is newString: ${newString}"
        // log.info "Here is messageRow: it's a ${messageRow.getClass().name}"
        // log.info "here are the keys: ${messageRow.keySet().toArray()}"
        log.info "About to send: EHLO ${outboundDomain}"
        output << "EHLO ${outboundDomain}\r\n"
        def doneWith250 = false
        def commandList = []
        while ( _not( doneWith250 ) ) {
            newString = reader.readLine()
            if ( _not( newString.matches( ".*[a-z].*" ) ) ) {
                commandList << newString.allButFirstFour()
            }
            log.info "Here is newString: ${newString}"
            if ( newString.startsWith( '250 ' ) ) { 
                doneWith250 = true 
            }
            log.info "doneWith250: ${doneWith250}"
        }
        log.info "Here is commandList: ${commandList}"
        log.info "About to send MAIL FROM:<${messageRow.from_address}>"
        output << "MAIL FROM:<${messageRow.from_address}>\r\n"
        newString = reader.readLine()
        log.info "Got response ${newString}"
        def got250ForRCPT = false
        def includeDSN = commandList.contains( 'DSN' )
        log.info "includeDSN: ${includeDSN}"
        def rcptEnd = includeDSN ? " NOTIFY=NEVER\r\n" : "\r\n"
        
        otherUserList.collect { oUser -> oUser.toLowerCase()
        }.each { uName ->
            log.info "About to send RCPT TO:<${uName}@${otherDomain}>${rcptEnd}"
            output << "RCPT TO:<${uName}@${otherDomain}>${rcptEnd}"
            newString = reader.readLine()
            log.info "Got response ${newString}"
            if ( newString.startsWith( "250" ) ) {
                got250ForRCPT = true
            }
        }
        if ( _not( got250ForRCPT ) ) {
            output << "QUIT\r\n"
        } else {
            output << "DATA\r\n"
            newString = reader.readLine()
            log.info "Here is response to DATA: ${newString}"
            if ( newString.startsWith( "354" ) ) {
                output << messageRow[ 'text_body' ]
                output << "\r\n.\r\n"
            }
            newString = reader.readLine()
            log.info "Here is newLine: ${newString}"
            output << "QUIT\r\n"
        }
        newString = reader.readLine()
        log.info "Here is newLine after QUIT: ${newString}"
        
        // log.info "Here is inputLine: ${inputLine}"
        /*
        while ( !areWeDone ) {
        }
        */
    }
} // end class

