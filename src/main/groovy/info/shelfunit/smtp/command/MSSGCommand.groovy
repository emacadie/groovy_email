package info.shelfunit.smtp.command

import groovy.sql.Sql
import groovy.util.logging.Slf4j 

import java.sql.SQLException

/**
There is no MSSG command in RFC 5321. But I needed something to handle the actual message after the DATA command, so here we are.
*/


@Slf4j
class MSSGCommand {
    
    static regex = '''(([\\w!#$%&’*+/=?`{|}~^-]+(?:\\.[\\w!#$%&’ # WTF?
*+/=?`{|}~^-]+)*)@((?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}))$(?x)'''    

    final Sql sql
    final List domainList
    MSSGCommand( def argSql, def argDomainList ) {
        log.info "Starting new MSSGCommand"
        log.info "Here is argDomainList: ${argDomainList}"
        this.sql = argSql
        this.domainList = argDomainList
    }
    
    def process( theMessage, prevCommandSet, bufferMap ) {
        log.info "In MSSGCommand"
        def resultString
        def resultMap = [:]
        resultMap.clear()

        if ( !prevCommandSet.lastSMTPCommandPrecedesMSSG() ) {
            resultMap.resultString = "503 Bad sequence of commands"
        } else {
            resultMap.resultString =  this.addMessageToDatabase( theMessage, bufferMap )
        }
        if ( resultMap.resultString == '250 OK' ) {
            resultMap.bufferMap = [:]
        }
        resultMap.prevCommandSet = prevCommandSet

		log.info "here is resultMap: ${resultMap.toString()}"
		resultMap
    } // process
    
    def addMessageToDatabase( theMessage, bufferMap ) {
        log.info "in addMessageToDatabase, here is bufferMap: ${bufferMap}"
        def result = '250 OK'
        def toAddresses = bufferMap.forwardPath
        def fromAddress = bufferMap.reversePath
        def insertCounts 
        def q = fromAddress =~ regex
        def fromDomain = q.getFromDomainInMSSG ()
        def sqlString
        if ( bufferMap.messageDirection == 'outbound' ) {
            log.info "It is an outbound message"
            sqlString = 'insert into mail_spool_out( id, from_address, to_address_list,  text_body, status_string, base_64_hash ) values (?, ?, ?, ?, ?, ?)'
        } else {
            sqlString = 'insert into mail_spool_in( id, from_address, to_address_list,  text_body, status_string, base_64_hash ) values (?, ?, ?, ?, ?, ?)'
        }
        try {
            sql.withTransaction {
                def wholeFromAddress = q.getWholeFromAddressInMSSG()
                log.info "here are the args: wholeFromAddress: ${wholeFromAddress}, toAddresses: ${toAddresses}, theMessage: ${theMessage}"
                log.info "About to call sql to enter message"
                insertCounts = sql.withBatch( sqlString ) { stmt ->
                    log.info "stmt is a ${stmt.class.name}"
                    stmt.addBatch( [ 
                        UUID.randomUUID(), // id, 
                        wholeFromAddress,  // from_address, 
                        toAddresses.join( ',' ), // to_address_list, 
                        theMessage,  // text_body, 
                        "ENTERED",    // status_string
                        bufferMap.userInfo?.base_64_hash ?: "" // base_64_hash
                    ] )
                }
            }
            log.info "Message delivered with no issues"
        } catch ( Exception e ) {
            log.info "Next exception message: ${e.getMessage()}"
            log.error "something went wrong", e
            result = '500 Something went wrong'
            SQLException ex = e.getNextException()
            log.info "Next exception message: ${ex.getMessage()}"
            log.error "something went wrong", ex 
        }
        
        log.info "here is insertCounts: ${insertCounts}"
        result
    } // addMessageToDatabase
}

