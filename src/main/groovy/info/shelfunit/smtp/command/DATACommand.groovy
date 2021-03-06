package info.shelfunit.smtp.command

import groovy.util.logging.Slf4j 

@Slf4j
class DATACommand {
    DATACommand() {
        log.info "starting new DATACommand"
    }
    
    def resultMap = [:]
    
    def process( theMessage, prevCommandSet, bufferMap ) {
        log.info  "In DATACommand"
        def resultString
        resultMap.clear()
        resultMap.bufferMap = bufferMap
        if ( theMessage.length() > 4 ) {
            resultMap.resultString = "501 Command not in proper form"
        } else if ( _not( theMessage.startsWith( 'DATA' ) )  ) {
            resultMap.resultString = "503 Bad sequence of commands"
        } else if ( _not ( prevCommandSet.lastSMTPCommandPrecedesDATA() ) ) {
            resultMap.resultString = "503 Bad sequence of commands"
        } else {
            prevCommandSet << "DATA"
            if ( bufferMap.handles8bit == "true" ) {
                resultMap.resultString = "354 Send 8BITMIME message, ending in <CRLF>.<CRLF>"
            } else {
                resultMap.resultString = "354 Start mail input; end with <CRLF>.<CRLF>"
            }
        }
        resultMap.prevCommandSet = prevCommandSet
        resultMap.bufferMap      = bufferMap
        // resultMap.rawCommandList << theMessage
        log.info "here is resultMap: ${resultMap.toString()}"
        resultMap
    }
}

