package info.shelfunit.postoffice.command

import groovy.sql.Sql
import groovy.util.logging.Slf4j 

@Slf4j
class STATCommand {

    final Sql sqlObject
    STATCommand( def argSql ) {
        log.info "Starting new STATCommand"
        this.sqlObject = argSql
    }
    
    def process( theMessage, prevCommandSet, bufferMap ) {
        log.info "Starting STATCommand.process"
        
        def resultString
        def resultMap = [:]
        resultMap.clear()
        
        if ( bufferMap.state != 'TRANSACTION' ) {
            resultMap.resultString = "-ERR Not in TRANSACTION state"
        } else if ( theMessage.toUpperCase() != 'STAT' ) {
            resultMap.resultString = "-ERR Command not in proper form"
        } else if ( !bufferMap.userInfo  ) {
            resultMap.resultString = "-ERR Command not in proper form"
        } else {
            if ( _not( bufferMap.hasSTATInfo() ) ) {
                bufferMap.getSTATInfo( sqlObject )
            }
            def userInfo  = bufferMap.userInfo
            def timestamp = bufferMap.timestamp
            resultMap.resultString = "+OK ${bufferMap.uuidList.size()} ${bufferMap.totalMessageSize}"
        }
        resultMap.bufferMap      = bufferMap
        resultMap.prevCommandSet = prevCommandSet
        
        log.info "here is resultMap: ${resultMap.toString()}"
        resultMap
    }
}

