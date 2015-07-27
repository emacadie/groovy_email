package info.shelfunit.socket.command


import groovy.util.logging.Slf4j 

@Slf4j
class RSETCommand {
    
    def process( theMessage, prevCommandSet, bufferMap ) {
        def resultString
        resultMap.clear()
        bufferMap.clear()
        prevCommandSet.clear()
        resultMap.bufferMap = bufferMap
		resultMap.prevCommandSet = prevCommandSet
		resultMap.resultString = "250 OK"
		log.info "here is resultMap: ${resultMap.toString()}"
		resultMap
    }
}

