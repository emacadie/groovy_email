package info.shelfunit.smtp.command


import groovy.util.logging.Slf4j 
// reg ex for email from http://www.mkyong.com/regular-expressions/how-to-validate-email-address-with-regular-expression/
// look at http://www.ngdc.noaa.gov/wiki/index.php?title=Regular_Expressions_in_Groovy as well
// RFC 5321 4.1.1 The reverse-path is the argument of the MAIL command, the forward-path is the argument of
//   the RCPT command, and the mail data is the argument of the DATA command. 

@Slf4j
class MAILCommand {
    
    // http://howtodoinjava.com/2014/11/11/java-regex-validate-email-address/
    static regex = '''^(MAIL FROM):<([\\w!#$%&’*+/=?`{|}~^-]+(?:\\.[\\w!#$%&’ # WTF?
*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6})>(\\s{0,}BODY=8BITMIME)?$(?x)'''

    static pattern = ~regex
    
    def process( theMessage, prevCommandSet, bufferMap ) {
        def resultString
        def resultMap = [:]
        resultMap.clear()
        def regexResult = ( theMessage ==~ pattern )
        if ( !prevCommandSet.lastSMTPCommandPrecedesMail() ) {
            resultMap.resultString = "503 Bad sequence of commands"
        } else if ( !theMessage.startsWith( 'MAIL FROM:' ) ) {
            resultMap.resultString = "501 Command not in proper form"
        } else if ( !regexResult ) {
            resultMap.resultString = "501 Command not in proper form"
        } else if ( !( theMessage ==~ pattern ) ) {
            resultMap.resultString = "501 Command not in proper form"
        } else {
            prevCommandSet << 'MAIL'
            
            def q = theMessage =~ pattern
            bufferMap?.clear()
            bufferMap.forwardPath = [] // for RCPT command
            bufferMap.reversePath =  q.getEmailAddressInMAIL()
            if ( q.handles8BitInMAIL() ) {
                bufferMap.handles8bit = "true"
                resultMap.resultString = "250 <${bufferMap.reversePath}> Sender and 8BITMIME OK"
            } else {
                resultMap.resultString = '250 OK'
            }
            log.info "here is reverse path: ${bufferMap.reversePath}"
            log.info "here is q: ${q}"
            resultMap.bufferMap = bufferMap
        }
        resultMap.prevCommandSet = prevCommandSet

		log.info "here is resultMap: ${resultMap.toString()}"
		resultMap
    }
}

