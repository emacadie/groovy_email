package info.shelfunit.smtp.command

import groovy.util.logging.Slf4j 

@Slf4j
class RSETCommand {
    
    def process( theMessage, prevCommandSet, bufferMap ) {
        def resultMap = [:]
        def resultString
        bufferMap.clear()
        prevCommandSet.clear()
        prevCommandSet << 'RSET'
        resultMap.bufferMap      = bufferMap
        resultMap.prevCommandSet = prevCommandSet
        resultMap.resultString   = "250 OK"
        log.info "here is resultMap: ${resultMap.toString()}"
        resultMap
    }
}

