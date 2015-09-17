package info.shelfunit.postoffice.command

import groovy.sql.Sql
import groovy.util.logging.Slf4j 
import java.sql.SQLException
import visibility.Hidden

@Slf4j
class QUITCommand {
    
    @Hidden def regex = "RETR\\s\\d+"
    
    @Hidden Sql sql
    @Hidden serverName
    
    QUITCommand( def argSql, def argServerName ) {
        log.info "Starting new QUITCommand"
        this.sql = argSql
        this.serverName = argServerName
    }
    
    def process( theMessage, prevCommandSet, bufferMap ) {
        log.info "Starting QUITCommand.process"
   
        def resultString
        def resultMap = [:]
        resultMap.clear()
        log.info "Here is bufferMap: ${bufferMap}"
        log.info "Does bufferMap.hasSTATInfo() sez the lolcat ? let's find out: ${bufferMap.hasSTATInfo()}"
        if ( !bufferMap.hasSTATInfo() ) {
            bufferMap.getSTATInfo( sql )
        }
        log.info "Does bufferMap.hasSTATInfo() sez the lolcat ? let's find out: ${bufferMap.hasSTATInfo()}"
        if ( theMessage != 'QUIT' ) {
            resultMap.resultString = "-ERR Command not in proper form"
        } else if ( bufferMap.state == 'AUTHORIZATION' ) {
            bufferMap.clear()
            resultMap.resultString = "+OK ${serverName}  POP3 server signing off"
        } else if ( bufferMap.state == 'TRANSACTION' ) {
            
            log.info "in the reg ex part"
            log.info "here is bufferMap in QUITCommand.process: ${bufferMap}"
            def idsToDelete = bufferMap.deleteMap.values() as List
            resultMap.resultString = "+OK ${serverName} POP3 server signing off"
            def qMarks = []
            def result = '250 OK'
            (1..idsToDelete.size()).each { qMarks << '?' }
            try {
                sql.execute "DELETE from mail_store where id in (${qMarks.join(',')})", idsToDelete
                bufferMap.clear()
                bufferMap.state = 'UPDATE'
            } catch ( Exception e ) {
                result = '500 Something went wrong'
                SQLException ex = e.getNextException()
                log.info "Next exception message: ${ex.getMessage()}"
                // ex.printStrackTrace()
                log.error "something went wrong", ex 
                // log.error "Failed to format {}", result, ex
            }
        }
        resultMap.bufferMap = bufferMap
        resultMap.prevCommandSet = prevCommandSet

		log.info "here is resultMap: ${resultMap.toString()}"
		resultMap
    }
}

