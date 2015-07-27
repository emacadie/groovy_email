package info.shelfunit.socket.command


import groovy.util.logging.Slf4j 

@Slf4j
class DATACommand {
    def resultMap = [:]
    
    def process( theMessage, prevCommandSet, bufferMap ) {
        def resultString
        resultMap.clear()
        bufferMap.clear()
        resultMap.bufferMap = bufferMap
        def domain = theMessage.getDomain()
        log.info "Here is the domain: ${domain} and it is a ${domain.class.name}"
        if ( domain.isMoreThan255Char() ) {
            resultMap.resultString = "501 Domain name length beyond 255 char limit per RFC 3696"
            resultMap.prevCommandSet = prevCommandSet
        } else if ( ( domain.is255CharOrLess() ) && ( theMessage.startsWithEHLO() ) ) {
            prevCommandSet.clear()
            prevCommandSet << "EHLO"
            resultMap.resultString = "250-Hello ${domain}\n250 HELP"
		} else if ( ( domain.is255CharOrLess() ) && ( theMessage.startsWithHELO() ) ) {
		    prevCommandSet.clear()
		    prevCommandSet << "HELO"
		    resultMap.resultString = "250 Hello ${domain}"
		}
		resultMap.prevCommandSet = prevCommandSet
		log.info "here is resultMap: ${resultMap.toString()}"
		resultMap
    }
}

