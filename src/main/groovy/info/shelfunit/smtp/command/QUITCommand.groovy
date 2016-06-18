package info.shelfunit.smtp.command

import groovy.util.logging.Slf4j 

@Slf4j
class QUITCommand {

    final List domainList
    QUITCommand( def argDomainList ) {
        log.info "Starting new QUITCommand"
        log.info "Here is argDomainList: ${argDomainList}"
        this.domainList = argDomainList
    }
    
    def process( theMessage, prevCommandSet, bufferMap ) {
        log.info "In QUITCommand"
        def resultMap = [:]
        resultMap.clear()
        
        bufferMap.clear()
        resultMap.bufferMap = [:]
        resultMap.resultString = "221 ${domainList[ 0 ]} Service closing transmission channel"
        
        resultMap.prevCommandSet = prevCommandSet
        
        log.info "here is resultMap: ${resultMap.toString()}"
        resultMap
    } // process
} // line 82

