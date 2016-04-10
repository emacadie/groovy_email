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
    
    def doWork( input, output, row, otherDomain, otherUserList  ) {
        log.info "In doWork"
        def areWeDone = false
        def inputLine
        def reader = input.newReader()
        log.info "About to read input line"
        // inputLine = input.read()
        def newString =  reader.readLine() 
        log.info "Here is newString: ${newString}"
        log.info "Here is row: it's a ${row.getClass().name}"
        // log.info "Here is inputLine: ${inputLine}"
        /*
        while ( !areWeDone ) {
        }
        */
    }
}

