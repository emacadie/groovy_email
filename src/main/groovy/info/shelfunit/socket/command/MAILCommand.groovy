package info.shelfunit.socket.command


import groovy.util.logging.Slf4j 

// RFC 5321 4.1.1 The reverse-path is the argument of the MAIL command, the forward-path is the argument of
//   the RCPT command, and the mail data is the argument of the DATA command. 

@Slf4j
class MAILCommand {
    def resulMap = [:]
    def process( theMessage, prevCommandList ) {
        def resultString
        // if () { }
        
    }
    
    def process( theMessage, prevCommandList, bufferMap ) {
        def resultString
        resultMap.clear()
        bufferMap.reversePath = '' 
        bufferMap.forwardPath = ''
        bufferMap.mailData = ''
        
        if ( !theMessage.startsWith( 'MAIL FROM:' ) ) {
            resultMap.resultString = "501 Command not in proper form"
        } else if ( !prevCommandList.last.matches( 'EHLO|HELO|RSET' ) ) {
            resultMap.resultString = "503 Bad sequence of commands"
        }
        
        def domain = theMessage.replaceFirst( 'EHLO |HELO ', '' )
        log.info "Here is the domain: ${domain}"
        if ( domain.length() > 255 ) {
            resultMap.resultString = "501 Domain name length beyond 255 char limit per RFC 3696"
            resultMap.prevCommandList = prevCommandList
        } else if ( ( domain.length() <= 255 ) && ( theMessage.firstFour() == 'EHLO' ) ) {
            prevCommandList.clear()
            prevCommandList << 'EHLO'
            resultMap.resultString = "250-Hello ${domain}\n250 HELP"
		} else if ( ( domain.length() <= 255 ) && ( theMessage.firstFour() == 'HELO' ) ) {
		    prevCommandList.clear()
		    prevCommandList << 'HELO'
		    resultMap.resultString = "250 Hello ${domain}"
		}
		resultMap.prevCommandList = prevCommandList
		resultMap.bufferMap = bufferMap
		log.info "here is resultMap: ${resultMap.toString()}"
		resultMap
    }
}

