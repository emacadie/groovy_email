package info.shelfunit.postoffice.command

import groovy.sql.Sql
import groovy.util.logging.Slf4j 

@Slf4j
class RSETCommand {

    final Sql sqlObject
    
    RSETCommand( def argSql ) {
        log.info "Starting new RSETCommand"
        this.sqlObject = argSql
    }
    
    def process( theMessage, prevCommandSet, bufferMap ) {
        log.info "Starting RSETCommand.process"
        
        def resultString
        def resultMap = [:]
        resultMap.clear()
        def deleteMap = bufferMap.deleteMap ?: [:]
        log.info "Here is bufferMap: ${bufferMap}"
        log.info "Does bufferMap.hasSTATInfo() sez the lolcat ? let's find out: ${bufferMap.hasSTATInfo()}"
        if ( !bufferMap.hasSTATInfo() ) {
            bufferMap.getSTATInfo( sqlObject )
        }
        log.info "Does bufferMap.hasSTATInfo() sez the lolcat ? let's find out: ${bufferMap.hasSTATInfo()}"
        if ( bufferMap.state != 'TRANSACTION' ) {
            resultMap.resultString = "-ERR Not in TRANSACTION state"
        } else if ( theMessage != 'RSET' ) {
            resultMap.resultString = "-ERR Command not in proper form"
        } else if ( theMessage == 'RSET' ) {
            log.info "in the reg ex part"
            bufferMap.each { k, v ->
                log.info "k is $k, here it is toString: ${k.toString()}"
            }
            bufferMap.deleteMap.clear()
            bufferMap.remove( deleteMap )
            resultMap.resultString = "+OK maildrop has ${bufferMap.uuidList.size()} messages (${bufferMap.totalMessageSize} octets)"
        }
        
        resultMap.bufferMap      = bufferMap
        resultMap.prevCommandSet = prevCommandSet
        
        log.info "here is resultMap: ${resultMap.toString()}"
        resultMap
    }
}

