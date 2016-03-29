package info.shelfunit.spool

import groovy.util.logging.Slf4j 
import info.shelfunit.mail.ConfigHolder
import java.io.IOException
import java.sql.SQLException
import fi.solita.clamav.ClamAVClient

@Slf4j
class InboundSpoolWorker{
    
    // states of message: ENTERED
    
    final config
    final sql
    ClamAVClient clamavj
    
    InboundSpoolWorker( ) {
    }
    
    def runClam( sql, clamavj ) {
        def cleanUUIDs = []
        def uncleanUUIDs = []
        sql.eachRow( "select * from mail_spool_in where status_string = ?", [ 'ENTERED' ] ) { row ->
            println "first 100 chars: ${row['text_body'].substring( 0, 100 )} "
            println "---------------------------------------------------------------------------------------\n\n"
            println "row['text_body'] is a ${row['text_body'].getClass().name}"
            byte[] data = row[ 'text_body' ].getBytes()
            InputStream input = new ByteArrayInputStream( data )
            println "input is a ${input.getClass().name}"
            def isClean = this.runClamOnMessage( input, clamavj )
            if ( isClean ) {
                cleanUUIDs << row[ 'id' ]
            } else {
                uncleanUUIDs << row[ 'id' ]
            }
        } // sql.eachRow
       this.updateStoreAfterClam( sql, cleanUUIDs, 'CLEAN' )
       this.updateStoreAfterClam( sql, uncleanUUIDs, 'UNCLEAN' )
    } // runClam
    
    def updateStoreAfterClam( sql, UUIDs, status ) {
        try {
            log.info "here is idsToDelete: ${UUIDs} and it is a ${UUIDs.getClass().name}"
            /* // keep around for a bit
            UUIDs.each{ uuid ->
                sql.executeUpdate "UPDATE mail_spool_in set status_string = ? where id = ? ", [ status, uuid ]
                log.info "Called the update command for ${uuid}"   
            }
            */
            def insertCounts 
            sql.withTransaction {
                insertCounts = sql.withBatch( "UPDATE mail_spool_in set status_string = ? where id = ? " ) { stmt ->
                    UUIDs.each{ uuid ->
                        stmt.addBatch( [ status, uuid ] )
                        log.info "Called the update command for ${uuid}"   
                    }
                }
            }
           
        } catch ( Exception e ) {
            log.error "Here is exception: ", e
            SQLException ex = e.getNextException()
            log.info "Next exception message: ${ex.getMessage()}"
            log.error "something went wrong", ex 
        }
    }
    
    def runClamOnMessage( inString, clamavj ) {
        def reply
        def messageIsClean = true
        try {
            reply = clamavj.scan( inString )
            log.info "here is reply: ${reply.toString()} and it's a ${reply.getClass().name}"
        } catch ( Exception e ) {
            messageIsClean = false
            throw new RuntimeException( "Could not scan the input", e )
        }
        log.info "ClamAVClient.isCleanReply( reply ) : ${ClamAVClient.isCleanReply( reply ) }"
        if ( !ClamAVClient.isCleanReply( reply ) ) {
            // throw new Exception( "aaargh. Something was found" )
            log.info "aaargh. Something was found"
            messageIsClean = false
        }
        return messageIsClean
    }
    
    
}


