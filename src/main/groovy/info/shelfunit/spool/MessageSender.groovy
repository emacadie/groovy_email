package info.shelfunit.spool

import groovy.util.logging.Slf4j 

@Slf4j
class MessageSender {
    
    MessageSender() {
        log.info "Starting new MessageSender"
    }
    
    def doWork( input, output, messageRow, otherDomain, otherUserList, outboundDomain ) {
        log.info "In doWork"
        log.info "output is a ${output.getClass().name}"
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
        output.send "EHLO ${outboundDomain}".checkForCRLF()
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
        output.send  "MAIL FROM:<${messageRow.from_address}>".checkForCRLF()
        newString = reader.readLine()
        log.info "Got response ${newString}"
        def got250ForRCPT = false
        def includeDSN = commandList.contains( 'DSN' )
        // try this and see if I can send to proton mail
        includeDSN = false 
        log.info "includeDSN: ${includeDSN}"
        def rcptEnd = includeDSN ? " NOTIFY=NEVER\r\n" : "\r\n"
        
        otherUserList.collect { oUser -> oUser.toLowerCase()
        }.each { uName ->
            log.info "About to send RCPT TO:<${uName}@${otherDomain}>${rcptEnd}"
            def rcpt = "RCPT TO:<${uName}@${otherDomain}>${rcptEnd}".checkForCRLF()
            log.info "and it's a " + "RCPT TO:<${uName}@${otherDomain}>${rcptEnd}"
            output.send "RCPT TO:<${uName}@${otherDomain}>${rcptEnd}".checkForCRLF()
            newString = reader.readLine()
            log.info "Got response ${newString}"
            if ( newString.startsWith( "250" ) ) {
                got250ForRCPT = true
            }
        }
        if ( _not( got250ForRCPT ) ) {
            output.send "QUIT".checkForCRLF()
        } else {
            output.send "DATA".checkForCRLF()
            newString = reader.readLine()
            log.info "Here is response to DATA: ${newString}"
            if ( newString.startsWith( "354" ) ) {
                output.send messageRow[ 'text_body' ]
                output.send "\r\n.\r\n"
            }
            newString = reader.readLine()
            log.info "Here is newLine: ${newString}"
            output.send "QUIT".checkForCRLF()
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

