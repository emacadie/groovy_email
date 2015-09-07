package info.shelfunit.postoffice.command

import groovy.sql.Sql
import groovy.util.logging.Slf4j 

import java.sql.Timestamp

import org.apache.shiro.crypto.hash.Sha512Hash

import visibility.Hidden

@Slf4j
class PASSCommand {
    
    @Hidden Sql sql
    PASSCommand( def argSql ) {
        log.info "Starting new PASSCommand"
        this.sql = argSql
    }

    static regex = '''^(PASS )(.*)$(?x)'''

    static pattern = ~regex
    
    def process( theMessage, prevCommandSet, bufferMap ) {
        log.info "Starting PASSCommand.process"
   
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
        } else {
            def userInfo = bufferMap.userInfo
            def password = q.getPasswordInPASS()
            def rawHash = new Sha512Hash( password, userInfo.username, userInfo.iterations ) 
            def finalHash = rawHash.toBase64()
            
            if ( userInfo.password_hash == finalHash ) { 
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
}

