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
        log.info "About to read input line"
        inputLine = input.read()
        log.info "Here are keys in row: ${row.keySet()}"
        log.info "Here is inputLine: ${inputLine}"
        while ( !areWeDone ) {
        }
    }
}

