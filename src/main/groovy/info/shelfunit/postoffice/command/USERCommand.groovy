package info.shelfunit.postoffice.command

import groovy.sql.Sql
import groovy.util.logging.Slf4j 

import visibility.Hidden

@Slf4j
class USERCommand {
    
    @Hidden Sql sql
    USERCommand( def argSql ) {
        log.info "Starting new USERCommand"
        this.sql = argSql
    }

    static regex = '''^(USER )([\\w!#$%&’*+/=?`{|}~^-]+(?:\\.[\\w!#$%&’ # WTF?
*+/=?`{|}~^-]+)*)$(?x)'''

    static pattern = ~regex
    
    def process( theMessage, prevCommandSet, bufferMap ) {
        log.info "In USERCommand, here is theMessage: ${theMessage}"
        def resultString
        def resultMap = [:]
        resultMap.clear()
        
        def regexResult = ( theMessage ==~ pattern )
        def q = theMessage =~ pattern
        if ( bufferMap.state != 'AUTHORIZATION' ) {
            resultMap.resultString = "-ERR Not in AUTHORIZATION state"
        } else if ( !theMessage.startsWith( 'USER ' ) ) {
            resultMap.resultString = "-ERR Command not in proper form A"
        } else if ( !regexResult ) {
            resultMap.resultString = "-ERR Command not in proper form B"
        } else if ( !( theMessage ==~ pattern ) ) {
            resultMap.resultString = "-ERR Command not in proper form C"
        } else {
            
            // log.info "Here is q[ 0 ][ 2 ]: ${q[0][2]}, Here is q[ 0 ][ 3 ]: ${q[0][3]}"
            def userName = theMessage.substring( 5 ) 
            // log.info "here is userName: ${userName}"
            def rows = sql.rows( 'select * from email_user where username=?', userName )
            // log.info "here is rows?.size() : ${rows?.size()} "
            log.info "Here is rows, it's a : ${rows.class.name}"
            if ( rows.size() != 0 ) { // row?.size() != null ) { //  != 0 ) {
                bufferMap.userInfo << rows[ 0 ]
                resultMap.resultString = "+OK ${userName} is a valid mailbox"
                // prevCommandSet << 'RCPT'
            } else {
                resultMap.resultString = "-ERR No such user ${userName}"
            }
            
        }
        resultMap.bufferMap = bufferMap
        resultMap.prevCommandSet = prevCommandSet

		log.info "here is resultMap: ${resultMap.toString()}"
		resultMap
    }
}

