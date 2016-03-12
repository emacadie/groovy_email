package info.shelfunit.smtp.command


import groovy.util.logging.Slf4j 
// reg ex for email from http://www.mkyong.com/regular-expressions/how-to-validate-email-address-with-regular-expression/
// look at http://www.ngdc.noaa.gov/wiki/index.php?title=Regular_Expressions_in_Groovy as well
// RFC 5321 4.1.1 The reverse-path is the argument of the MAIL command (who it's from), 
// the forward-path is the argument of the RCPT command (who it's to),
// and the mail data is the argument of the DATA command. 

@Slf4j
class MAILCommand {
    
    final List domainList
    MAILCommand( argDomainList ) {
        log.info "Starting new MAILCommand"
        println "Here is argDomainList: ${argDomainList}"
        this.domainList = argDomainList
    }
    
    // http://howtodoinjava.com/2014/11/11/java-regex-validate-email-address/
    static mailFrom = '''^(MAIL FROM):<'''
    static localPart = '''([\\w!#$%&’*+/=?`{|}~^-]+(?:\\.[\\w!#$%&’*+/=?`{|}~^-]+)*@'''
    static domain = '''((?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}))>'''
    static eightBitRuntime = '''(\\s{0,}BODY=8BITMIME)?$(?x)'''
    static regex = mailFrom + localPart + domain + eightBitRuntime

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
            if ( domainList.containsIgnoreCase( q.getDomainInMAIL() ) ) {
                bufferMap.messageDirection = "outbound"
            } else {
                bufferMap.messageDirection = "inbound"
            }
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

