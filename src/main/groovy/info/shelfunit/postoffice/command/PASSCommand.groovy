package info.shelfunit.postoffice.command

import groovy.sql.Sql
import groovy.util.logging.Slf4j 

import org.apache.shiro.crypto.hash.Sha512Hash

import visibility.Hidden

@Slf4j
class PASSCommand {
    
    @Hidden Sql sql
    PASSCommand( def argSql ) {
        log.info "Starting new USERCommand"
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
        if ( bufferMap.state != 'AUTHORIZATION' ) {
            resultMap.resultString = "-ERR Not in AUTHORIZATION state"
        } else if ( !theMessage.startsWith( 'PASS ' ) ) {
            resultMap.resultString = "-ERR Command not in proper form A"
        } else if ( !regexResult ) {
            resultMap.resultString = "-ERR Command not in proper form B"
        } else if ( !( theMessage ==~ pattern ) ) {
            resultMap.resultString = "-ERR Command not in proper form C"
        } else {
            def userInfo = bufferMap.userInfo
            def password = q[ 0 ][ 2 ]
            def rawHash = new Sha512Hash( password, userInfo.username, userInfo.iterations ) 
            def finalHash = rawHash.toBase64()
            
            if ( userInfo.password_hash == finalHash ) { // row?.size() != null ) { //  != 0 ) {
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

