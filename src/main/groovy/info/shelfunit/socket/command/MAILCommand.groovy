package info.shelfunit.socket.command


import groovy.util.logging.Slf4j 
// reg ex for email from http://www.mkyong.com/regular-expressions/how-to-validate-email-address-with-regular-expression/
// look at http://www.ngdc.noaa.gov/wiki/index.php?title=Regular_Expressions_in_Groovy as well
// RFC 5321 4.1.1 The reverse-path is the argument of the MAIL command, the forward-path is the argument of
//   the RCPT command, and the mail data is the argument of the DATA command. 

@Slf4j
class MAILCommand {
    
    // http://howtodoinjava.com/2014/11/11/java-regex-validate-email-address/
    static regex = '''^(MAIL FROM):<([\\w!#$%&’*+/=?`{|}~^-]+(?:\\.[\\w!#$%&’ # WTF?
*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6})>$(?x)'''
/*
 regex = '''^(MAIL FROM):<([\\w!#$%&’*+/=?`{|}~^-]+(?:\\.[\\w!#$%&’ # WTF?
*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+([a-zA-Z]{2,6}))>$(?x)'''
regexB = '''^(MAIL FROM):<[\\w!#$%&’*+/=?`{|}~^-]+(?:\\.[\\w!#$%&’*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}>$'''
*/
    static pattern = ~regex
    
    def resultMap = [:]
    
    def process( theMessage, prevCommandList, bufferMap ) {
        def resultString
        resultMap.clear()
        // bufferMap.reversePath = '' 
        // bufferMap.forwardPath = ''
        // bufferMap.mailData = ''
        def regexResult = ( theMessage ==~ pattern )
        if ( !prevCommandList.last().matches( 'EHLO|HELO|RSET' ) ) {
            resultMap.resultString = "503 Bad sequence of commands"
        } else if ( !theMessage.startsWith( 'MAIL FROM:' ) ) {
            resultMap.resultString = "501 Command not in proper form"
        } else if ( !regexResult ) {
            resultMap.resultString = "501 Command not in proper form"
        } else if ( !( theMessage ==~ pattern ) ) {
            resultMap.resultString = "501 Command not in proper form"
        } else {
            prevCommandList << 'MAIL'
            resultMap.resultString = '250 OK'
            def q = theMessage =~ pattern
            // log.info "Here is q: ${q}"
            // log.info "Here is q[0][2]: ${q[0][2]}"
            bufferMap.clear()
            bufferMap.reversePath =  q[ 0 ][ 2 ]
            resultMap.bufferMap = bufferMap
        }
        resultMap.prevCommandList = prevCommandList

		log.info "here is resultMap: ${resultMap.toString()}"
		resultMap
    }
}

