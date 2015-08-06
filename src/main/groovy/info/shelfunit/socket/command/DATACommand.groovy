package info.shelfunit.socket.command


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
        } else if ( !theMessage.startsWith( 'DATA' )  ) {
            resultMap.resultString = "503 Bad sequence of commands"
        } else if ( !prevCommandSet.lastCommandPrecedesDATA() ) {
            resultMap.resultString = "503 Bad sequence of commands"
        } else {
		    prevCommandSet << "DATA"
		    resultMap.resultString = "354 Start mail input; end with <CRLF>.<CRLF>"
		}
		resultMap.prevCommandSet = prevCommandSet
		resultMap.bufferMap = bufferMap
		log.info "here is resultMap: ${resultMap.toString()}"
		resultMap
    }
}

