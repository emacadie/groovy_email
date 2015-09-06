package info.shelfunit.postoffice.command

import groovy.sql.Sql
import groovy.util.logging.Slf4j 

import visibility.Hidden

@Slf4j
class STATCommand {
    
    @Hidden Sql sql
    STATCommand( def argSql ) {
        log.info "Starting new USERCommand"
        this.sql = argSql
    }
    
    def process( theMessage, prevCommandSet, bufferMap ) {
        log.info "Starting STATCommand.process"
   
        def resultString
        def resultMap = [:]
        resultMap.clear()
        if ( !bufferMap.hasSTATInfo() ) {
            bufferMap.getSTATInfo( sql )
        }

        if ( bufferMap.state != 'TRANSACTION' ) {
            resultMap.resultString = "-ERR Not in TRANSACTION state"
        } else if ( theMessage != 'STAT' ) {
            resultMap.resultString = "-ERR Command not in proper form"
        } else {
            def userInfo = bufferMap.userInfo
            def timestamp = bufferMap.timestamp
            
            resultMap.resultString = "+OK ${bufferMap.uuidList.size()} ${bufferMap.totalMessageSize}"
            
        }
        resultMap.bufferMap = bufferMap
        resultMap.prevCommandSet = prevCommandSet

		log.info "here is resultMap: ${resultMap.toString()}"
		resultMap
    }
}

