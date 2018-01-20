package info.shelfunit.postoffice.command

import groovy.sql.Sql
import groovy.util.logging.Slf4j 

@Slf4j
class USERCommand {

    final Sql sqlObject
    USERCommand( def argSql ) {
        log.info "Starting new USERCommand"
        this.sqlObject = argSql
    }
    
    static regex = '''^(USER |user )([\\w!#$%&’*+/=?`{|}~^-]+(?:\\.[\\w!#$%&’ # WTF?
    *+/=?`{|}~^-]+)*)$(?x)(?i)'''
    
    static pattern = ~regex
    
    def process( theMessage, prevCommandSet, bufferMap ) {
        log.info "In USERCommand, here is theMessage: ${theMessage}"
        
        def resultString
        def resultMap = [:]
        resultMap.clear()
        def startsWithUser = theMessage.toUpperCase().startsWith( 'USER' )
        
        def regexResult = ( theMessage ==~ pattern )
        def q = theMessage =~ pattern
        if ( bufferMap.state != 'AUTHORIZATION' ) {
            resultMap.resultString = "-ERR Not in AUTHORIZATION state"
        } else if ( _not( startsWithUser ) ) { // ( ( !theMessage.startsWith( 'USER ' ) ) && !theMessage.startsWith( 'user ' ) ) {
            log.info "Command does not start with user!"
            resultMap.resultString = "-ERR Command not in proper form"
        } else if ( _not( regexResult ) ) {
            log.info "not meeting regex result, returning error"
            resultMap.resultString = "-ERR Command not in proper form"
        } else if ( _not( theMessage ==~ pattern ) ) {
            log.info "Command does not fit pattern"
            resultMap.resultString = "-ERR Command not in proper form"
        } else {
        
            def userName = theMessage.substring( 5 ) // convert to regex?
            def rows     = sqlObject.rows( 
                "select * from email_user where username_lc = ?", 
                userName.toLowerCase() 
            )
            log.info "Here is rows, it's a : ${rows.class.name}"
            if ( rows.size() != 0 ) { 
                bufferMap.userInfo = rows[ 0 ]
                resultMap.resultString = "+OK ${userName} is a valid mailbox"
            } else {
                resultMap.resultString = "-ERR No such user ${userName}"
            }
        }
        resultMap.bufferMap      = bufferMap
        resultMap.prevCommandSet = prevCommandSet
        
        log.info "here is resultMap: ${resultMap.toString()}"
        resultMap
    }
}


