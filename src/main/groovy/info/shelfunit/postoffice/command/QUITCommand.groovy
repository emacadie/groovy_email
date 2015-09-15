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
            bufferMap.state = 'UPDATE'
            log.info "in the reg ex part"
            log.info "here is bufferMap in QUITCommand.process: ${bufferMap}"
            def idsToDelete = bufferMap.deleteMap.values()
            resultMap.resultString = "+OK ${serverName}  POP3 server signing off"
            def qMarks = []
            (1..idsToDelete.size()).each { qMarks << '?' }
            try {
                sql.execute "DELETE from mail_store where id in (${qMarks.join(',')})", idsToDelete
                /*
                sql.withTransaction {
                    idsToDelete.eachWithIndex { address, i ->
                        def q = address =~ regex
                        def wholeAddress = q.getWholeAddressInMSSG()
                        def userName = q.getUserNameInMSSG()
                        log.info "here are the args: [ uuidSet[ i ]: ${uuidSet[ i ]}, userName: ${userName}, fromAddress: ${fromAddress}, wholeAddress: ${wholeAddress}, theMessage: ${theMessage}"
                        insertCounts = sql.withBatch( 'insert into mail_store(id, username, from_address, to_address, text_body) values (?, ?, ?, ?, ?)' ) { stmt ->
                            log.info "uuidSet[ i ] is a ${uuidSet[ i ].class.name}"
                            log.info "stmt is a ${stmt.class.name}"
                            // stmt.setObject( 1, uuidSet[ i ] )
                            // stmt.setBlob( 5, theMessage )
                            stmt.addBatch( [ uuidSet[ i ], userName, fromAddress, wholeAddress, theMessage ] )
                        }
                    }
                }
                */
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

