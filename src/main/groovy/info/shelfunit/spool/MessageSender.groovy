package info.shelfunit.spool

import groovy.util.logging.Slf4j 
import info.shelfunit.mail.ConfigHolder
import java.io.IOException
import java.sql.SQLException

@Slf4j
class MessageSender {
    
    MessageSender() {
    }
    
    def doWork( input, output, row, otherDomain, otherUserList  ) {
        def areWeDone = false
        def inputLine
        inputLine = input.read()
        log.info "Here is inputLine: ${inputLine}"
        while ( !areWeDone ) {
        }
    }
}

