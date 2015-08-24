package info.shelfunit.smtp.command

import groovy.sql.Sql
import groovy.util.logging.Slf4j 

import visibility.Hidden

@Slf4j
class RCPTCommand {
    
    @Hidden Sql sql
    @Hidden List domainList
    RCPTCommand( def argSql, def argDomainList ) {
        log.info "Starting new RCPTCommand"
        println "Here is argDomainList: ${argDomainList}"
        this.sql = argSql
        this.domainList = argDomainList
    }
    
    static regex = '''^(RCPT TO):<([\\w!#$%&’*+/=?`{|}~^-]+(?:\\.[\\w!#$%&’ # WTF?
*+/=?`{|}~^-]+)*@((?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}))>$(?x)'''
/*
q[0][3] gives domain

q[0][2] gives whole address

 regex = '''^(MAIL FROM):<([\\w!#$%&’*+/=?`{|}~^-]+(?:\\.[\\w!#$%&’ # WTF?
*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+([a-zA-Z]{2,6}))>$(?x)'''
regexB = '''^(MAIL FROM):<[\\w!#$%&’*+/=?`{|}~^-]+(?:\\.[\\w!#$%&’*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}>$'''
*/
    static pattern = ~regex
    
    def process( theMessage, prevCommandSet, bufferMap ) {
        log.info "In RCPTCommand"
        def resultString
        def resultMap = [:]
        resultMap.clear()
        
        def regexResult = ( theMessage ==~ pattern )
        def q = theMessage =~ pattern
        if ( !prevCommandSet.lastCommandPrecedesRCPT() ) {
            resultMap.resultString = "503 Bad sequence of commands"
        } else if ( !theMessage.startsWith( 'RCPT TO:' ) ) {
            resultMap.resultString = "501 Command not in proper form"
        } else if ( !regexResult ) {
            resultMap.resultString = "501 Command not in proper form"
        } else if ( !( theMessage ==~ pattern ) ) {
            resultMap.resultString = "501 Command not in proper form"
        } else if ( !domainList.includes( q.extractDomain() ) ) {
            resultMap.resultString = "550 No such user"
        } else {
            
            // log.info "Here is q[ 0 ][ 2 ]: ${q[0][2]}, Here is q[ 0 ][ 3 ]: ${q[0][3]}"
            def userName = q.extractUserNameInRCPT() 
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
            
        }
        resultMap.bufferMap = bufferMap
        resultMap.prevCommandSet = prevCommandSet

		log.info "here is resultMap: ${resultMap.toString()}"
		resultMap
    }
}
