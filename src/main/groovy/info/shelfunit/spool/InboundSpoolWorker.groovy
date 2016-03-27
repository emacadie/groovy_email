package info.shelfunit.spool

import groovy.sql.Sql
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
        this.config = ConfigHolder.instance.getConfObject()
        def db = ConfigHolder.instance.returnDbMap()         
        sql = Sql.newInstance( db.url, db.user, db.password, db.driver )
        def host = config.clamav.hostname
        def port = config.clamav.port
        clamavj = createClamAVClient( host, port )
    }
    
    def createClamAVClient( host, port ) {
        return new ClamAVClient( host, port.toInt() )
    }
    
    def doWork() {
        if ( checkClam() ) {
            sql.eachRow( "select * from mail_spool_in where status_string = ?", [ 'ENTERED' ] ) { row ->
                println "${row['text_body']} "
                println "---------------------------------------------------------------------------------------\n\n"
                println "row['text_body'] is a ${row['text_body'].getClass().name}"
                byte[] data = row[ 'text_body' ].getBytes()
                InputStream input = new ByteArrayInputStream( data )
                println "input is a ${input.getClass().name}"
                // byte[] reply
                def reply
                try {
                    reply = clamavj.scan( input )
                    log.info "here is reply: ${reply.toString()}"
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
    
    def checkClam() {
        def result
        try {
            result = clamavj.ping()
        } catch ( IOException ioEx ) {
            result = false
        }
        log.info "result is ${result}"
        result
    }
}


