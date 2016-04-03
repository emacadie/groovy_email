package info.shelfunit.smtp.command

import groovy.util.logging.Slf4j 

@Slf4j
class RCPTCommand {

    final domainList
    RCPTCommand( def argDomainList ) {
        log.info "Starting new RCPTCommand, here is argDomainList: ${argDomainList}"
        this.domainList = argDomainList
    }

    static rcptTo     = '''^(RCPT TO):<'''
    static localPart  = '''([\\w!#$%&’*+/=?`{|}~^-]+(?:\\.[\\w!#$%&’*+/=?`{|}~^-]+)*@'''
    static domainName = '''((?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}))>$(?x)'''
    static regex = rcptTo + localPart + domainName 

    def process( theMessage, prevCommandSet, bufferMap ) {
        log.info "In RCPTCommand"
        def resultMap = [:]
        resultMap.clear()
        
        def regexResult = ( theMessage ==~ regex ) 
        def q = theMessage =~ regex 
        if ( !prevCommandSet.lastSMTPCommandPrecedesRCPT() ) {
            resultMap.resultString = "503 Bad sequence of commands"
        } else if ( !theMessage.startsWith( 'RCPT TO:' ) ) {
            resultMap.resultString = "501 Command not in proper form"
        } else if ( !regexResult ) {
            resultMap.resultString = "501 Command not in proper form"
        } else if ( !( theMessage ==~ regex ) ) {
            resultMap.resultString = "501 Command not in proper form"
        } else if ( !domainList.containsIgnoreCase( q.extractDomainRCPT() ) && ( bufferMap.messageDirection == 'inbound' ) ) {
            resultMap.resultString = "550 No such user" // make it case insensitive here
        } else {
            bufferMap.forwardPath << q.getEmailAddressInRCPT() 
            resultMap.resultString = '250 OK'
            prevCommandSet << 'RCPT'
        }
        resultMap.bufferMap = bufferMap
        resultMap.prevCommandSet = prevCommandSet

		log.info "here is resultMap: ${resultMap.toString()}"
		resultMap
    }
}
/* def userName = q.extractUserNameInRCPT() 
            // log.info "here is userName: ${userName}"
            def rows = sql.rows( 'select * from email_user where username=?', userName )
            // log.info "here is rows?.size() : ${rows?.size()} "
            if ( rows.size() != 0 ) { // row?.size() != null ) { //  != 0 ) {
                bufferMap.forwardPath << q.getEmailAddressInRCPT() 
                resultMap.resultString = '250 OK'
                prevCommandSet << 'RCPT'
            } else {
                resultMap.resultString = "550 No such user"
            }
*/

