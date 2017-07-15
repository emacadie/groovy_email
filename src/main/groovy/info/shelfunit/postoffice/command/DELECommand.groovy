package info.shelfunit.postoffice.command

import groovy.sql.Sql
import groovy.util.logging.Slf4j 

@Slf4j
class DELECommand {
    
    final def regex = "DELE\\s\\d+"
    final Sql sql
    
    DELECommand( def argSql ) {
        log.info "Starting new DELECommand"
        this.sql = argSql
    }
    
    def process( theMessage, prevCommandSet, bufferMap ) {
        log.info "Starting DELECommand.process"
        
        def resultString
        def resultMap = [:]
        resultMap.clear()
        def deleteMap = bufferMap.deleteMap ?: [:]
        log.info "Here is bufferMap: ${bufferMap}"
        log.info "Does bufferMap.hasSTATInfo() sez the lolcat ? let's find out: ${bufferMap.hasSTATInfo()}"
        if ( !bufferMap.hasSTATInfo() ) {
            bufferMap.getSTATInfo( sql )
        }
        log.info "Does bufferMap.hasSTATInfo() sez the lolcat ? let's find out: ${bufferMap.hasSTATInfo()}"
        if ( bufferMap.state != 'TRANSACTION' ) {
            resultMap.resultString = "-ERR Not in TRANSACTION state"
        } else if ( !theMessage.matches( regex ) ) {
            resultMap.resultString = "-ERR Command not in proper form"
        } else if ( theMessage.matches( regex ) ) {
            log.info "in the reg ex part"
            def messageNum = theMessage.getIntInPOP3Command() 
            if ( messageNum > bufferMap.uuidList.size() ) {
                resultMap.resultString = "-ERR no such message, only ${bufferMap.uuidList.size()} messages in maildrop"
            } else {
                def uuid = bufferMap.uuidList[ messageNum - 1 ].id
                log.info "here is bufferMap.uuidList: ${bufferMap.uuidList}"
                log.info "uuid is a ${uuid.getClass().name}"
                if ( deleteMap.containsKey( messageNum ) ) {
                    resultMap.resultString = "-ERR message ${messageNum} already deleted"
                } else {
                    deleteMap[ messageNum ] = bufferMap.uuidList[ messageNum - 1 ].id
                    resultMap.resultString = "+OK message ${messageNum} deleted"
                }
            }
        }
        bufferMap.deleteMap      = deleteMap
        resultMap.bufferMap      = bufferMap
        resultMap.prevCommandSet = prevCommandSet
        
        log.info "here is resultMap: ${resultMap.toString()}"
        resultMap
    }
}

