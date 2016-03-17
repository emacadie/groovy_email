package info.shelfunit.smtp.command


import groovy.util.logging.Slf4j 
// reg ex for email from http://www.mkyong.com/regular-expressions/how-to-validate-email-address-with-regular-expression/
// look at http://www.ngdc.noaa.gov/wiki/index.php?title=Regular_Expressions_in_Groovy as well
// RFC 5321 4.1.1 The reverse-path is the argument of the MAIL command (who it's from), 
// the forward-path is the argument of the RCPT command (who it's to),
// and the mail data is the argument of the DATA command. 

@Slf4j
class AUTHCommand {
    
    final List domainList
    final sql
    AUTHCommand( argSql, argDomainList ) {
        log.info "Starting new AUTHCommand"
        println "Here is argDomainList: ${argDomainList}"
        this.domainList = argDomainList
        this.sql = argSql
    }
    
    // http://howtodoinjava.com/2014/11/11/java-regex-validate-email-address/
    static firstPart = '''^(AUTH PLAIN) '''
    static encryptedPart = '''(.*)'''
    static regex = firstPart + encryptedPart

    static pattern = ~regex
    
    def process( theMessage, prevCommandSet, bufferMap ) {
        def resultString
        def resultMap = [:]
        resultMap.clear()
        def regexResult = ( theMessage ==~ pattern )
        if ( prevCommandSet.contains( 'AUTH' ) ) {
            resultMap.resultString = "503 Bad sequence of commands"
        } else if ( !theMessage.startsWith( 'AUTH PLAIN' ) ) {
            resultMap.resultString = "501 Command not in proper form"
        } else if ( !regexResult ) {
            resultMap.resultString = "501 Command not in proper form"
        } else if ( !( theMessage ==~ pattern ) ) {
            resultMap.resultString = "501 Command not in proper form"
        } else {
            def q = theMessage =~ pattern
            def base64Part = q.getBase64InAuth()
            def userInfoFrombase64 = sql.firstRow( 'select * from email_user where base_64_hash=?', base64Part )
            if ( userInfoFrombase64 ) {
                bufferMap.userInfo = userInfoFrombase64
                resultMap.resultString = "235 2.7.0 Authentication successful"
                prevCommandSet << 'AUTH'
            } else {
                resultMap.resultString = "535 5.7.8  Authentication credentials invalid"
            }
            
            log.info "here is q: ${q}"
            
        }
        resultMap.prevCommandSet = prevCommandSet
        resultMap.bufferMap = bufferMap

		log.info "here is resultMap: ${resultMap.toString()}"
		resultMap
    }
}

