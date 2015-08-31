package info.shelfunit.postoffice.command

import groovy.sql.Sql
import groovy.util.logging.Slf4j 

import org.apache.shiro.crypto.hash.Sha512Hash

import visibility.Hidden

@Slf4j
class STATCommand {
    
    @Hidden Sql sql
    STATCommand( def argSql ) {
        log.info "Starting new USERCommand"
        this.sql = argSql
    }

    static regex = '''^(STAT )(.*)$(?x)'''

    static pattern = ~regex
    
    def process( theMessage, prevCommandSet, bufferMap ) {
        log.info "Starting STATCommand.process"
   
        def resultString
        def resultMap = [:]
        resultMap.clear()
        
        def regexResult = ( theMessage ==~ pattern )
        def q = theMessage =~ pattern
        if ( bufferMap.state != 'TRANSACTION' ) {
            resultMap.resultString = "-ERR Not in TRANSACTION state"
        } else if ( !theMessage.startsWith( 'STAT' ) ) {
            resultMap.resultString = "-ERR Command not in proper form"
        } else if ( !theMessage.length() == 4 ) {
            resultMap.resultString = "-ERR Command not in proper form"
        } else if ( !( theMessage ==~ pattern ) ) {
            resultMap.resultString = "-ERR Command not in proper form"
        } else {
            def userInfo = bufferMap.userInfo
            def password = q.getPasswordInPASS()
            def rawHash = new Sha512Hash( password, userInfo.username, userInfo.iterations ) 
            def finalHash = rawHash.toBase64()
            
            if ( userInfo.password_hash == finalHash ) { 
                resultMap.resultString = "+OK ${userInfo.username} authenticated"
                bufferMap.state = 'TRANSACTION'
            } else {
                resultMap.resultString = "-ERR ${userInfo.username} not authenticated"
            }
            
        }
        resultMap.bufferMap = bufferMap
        resultMap.prevCommandSet = prevCommandSet

		log.info "here is resultMap: ${resultMap.toString()}"
		resultMap
    }
}

