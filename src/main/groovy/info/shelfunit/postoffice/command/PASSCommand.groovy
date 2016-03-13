package info.shelfunit.postoffice.command

import groovy.util.logging.Slf4j 

import java.sql.SQLException
import java.sql.Timestamp

import org.apache.shiro.crypto.hash.Sha512Hash

@Slf4j
class PASSCommand {
    
    final sql
    
    PASSCommand( argSql ) {
        log.info "Starting new PASSCommand"
        this.sql = argSql
    }

    static regex = '''^(PASS )(.*)$(?x)'''

    static pattern = ~regex
    
    def process( theMessage, prevCommandSet, bufferMap ) {
        log.info "Starting PASSCommand.process"
        log.info "Here is bufferMap.userInfo: ${bufferMap.userInfo}"
        def resultString
        def resultMap = [:]
        resultMap.clear()
        
        def regexResult = ( theMessage ==~ pattern )
        def q = theMessage =~ pattern
        bufferMap.timestamp = null
        if ( bufferMap.state != 'AUTHORIZATION' ) {
            resultMap.resultString = "-ERR Not in AUTHORIZATION state"
        } else if ( !theMessage.startsWith( 'PASS ' ) ) {
            resultMap.resultString = "-ERR Command not in proper form"
        } else if ( !regexResult ) {
            resultMap.resultString = "-ERR Command not in proper form"
        } else if ( !( theMessage ==~ pattern ) ) {
            resultMap.resultString = "-ERR Command not in proper form"
        } else if ( !bufferMap.userInfo ) {
            resultMap.resultString = "-ERR Command not in proper form - No user sent"
        } else {
            def userInfo = bufferMap.userInfo
            def password = q.getPasswordInPASS()
            def rawHash = new Sha512Hash( password, userInfo.username, userInfo.iterations.toInteger() ) 
            def finalHash = rawHash.toBase64()
            log.info "here is bufferMap.userInfo.userid: ${bufferMap.userInfo.userid} and it's a ${bufferMap.userInfo.userid.getClass().name}"
            if ( ( userInfo.password_hash == finalHash ) && 
            ( this.changeLoggedInFlag( bufferMap.userInfo.userid ) == '250 OK' ) ) { 
                resultMap.resultString = "+OK ${userInfo.username} authenticated"
                bufferMap.state = 'TRANSACTION'
                bufferMap.timestamp = Timestamp.create()
            } else {
                resultMap.resultString = "-ERR ${userInfo.username} not authenticated"
            }
            
        }
        resultMap.bufferMap = bufferMap
        resultMap.prevCommandSet = prevCommandSet

		log.info "here is resultMap: ${resultMap.toString()}"
		resultMap
    }
    
    private changeLoggedInFlag( def argUserid ) {
        def result = '250 OK'
        log.info "in changedLoggedInFlag with arg ${argUserid}"
        try {
            sql.executeUpdate "UPDATE email_user set logged_in = ? where userid = ?", [ true, argUserid ]
        } catch ( SQLException ex ) {
            result = '500 Something went wrong'
        }
        
        return result
    }
}

