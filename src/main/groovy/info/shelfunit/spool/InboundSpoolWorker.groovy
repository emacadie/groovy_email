package info.shelfunit.spool

import groovy.util.logging.Slf4j 
import info.shelfunit.mail.ConfigHolder
import java.io.IOException
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
        sql.eachRow( "select * from mail_spool_in where status_string = ?", [ 'ENTERED' ] ) { row ->
            println "first 100 chars: ${row['text_body'].substring( 0, 100 )} "
            println "---------------------------------------------------------------------------------------\n\n"
            println "row['text_body'] is a ${row['text_body'].getClass().name}"
            byte[] data = row[ 'text_body' ].getBytes()
            InputStream input = new ByteArrayInputStream( data )
            println "input is a ${input.getClass().name}"
            def reply
            try {
                reply = clamavj.scan( input )
                log.info "here is reply: ${reply.toString()} and it's a ${reply.getClass().name}"
            } catch ( Exception e ) {
                throw new RuntimeException( "Could not scan the input", e )
            }
            log.info "ClamAVClient.isCleanReply( reply ) : ${ClamAVClient.isCleanReply( reply ) }"
            if ( !ClamAVClient.isCleanReply( reply ) ) {
                throw new Exception( "aaargh. Something was found" )
            }
        } // sql.eachRow
       
    }
    
    
}


