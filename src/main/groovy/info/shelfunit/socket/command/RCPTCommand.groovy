package info.shelfunit.socket.command

import groovy.sql.Sql
import groovy.util.logging.Slf4j 

@Slf4j
class RCPTCommand {
    
    Sql sql
    List domainList
    RCPTCommand( def argSql, def argDomainList  ) {
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
    
    def resultMap = [:]
    
    def process( theMessage, prevCommandList, bufferMap ) {
        def resultString
        resultMap.clear()
        
        def regexResult = ( theMessage ==~ pattern )
        if ( !prevCommandList.lastCommandPrecedesRCPT() ) {
            resultMap.resultString = "503 Bad sequence of commands"
        } else if ( !theMessage.startsWith( 'RCPT TO:' ) ) {
            resultMap.resultString = "501 Command not in proper form"
        } else if ( !regexResult ) {
            resultMap.resultString = "501 Command not in proper form"
        } else if ( !( theMessage ==~ pattern ) ) {
            resultMap.resultString = "501 Command not in proper form"
        } else {
            prevCommandList << 'RCPT'
            def q = theMessage =~ pattern
            log.info "q is a ${q.class.name}"
            log.info "Here is q[ 0 ][ 2 ]: ${q[0][2]}"
            log.info "Here is q[ 0 ][ 3 ]: ${q[0][3]}"
            def userName = q.extractUserName() 
            log.info "here is userName: ${userName}"
            def rows = sql.rows( 'select * from email_user where username=?', userName )
            log.info "here is rows?.size() : ${rows?.size()} "
            if ( rows.size() != 0 ) { // row?.size() != null ) { //  != 0 ) {
                bufferMap.forwardPath = q.getEmailAddress() // q[ 0 ][ 2 ]
                resultMap.resultString = '250 OK'
            } else {
                resultMap.resultString = "550 No such user"
            }
            
            // bufferMap.clear()
            // bufferMap.reversePath =  q[ 0 ][ 2 ]
            
        }
        resultMap.bufferMap = bufferMap
        resultMap.prevCommandList = prevCommandList

		log.info "here is resultMap: ${resultMap.toString()}"
		resultMap
    }
}

