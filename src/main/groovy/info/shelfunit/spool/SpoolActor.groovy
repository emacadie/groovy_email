package info.shelfunit.spool

import groovy.sql.Sql
import groovy.util.logging.Slf4j 

import groovyx.gpars.actor.DynamicDispatchActor

import info.shelfunit.mail.ConfigHolder

@Slf4j
class SpoolActor extends DynamicDispatchActor {
    
    void onMessage( SpoolRunnerMessage message ) {
        log.info "About to start the spooler"
        def keepGoing = true
        while ( keepGoing ) {
            sleep( 60.seconds() )
            log.info "still going in runWithActors"
            def db = ConfigHolder.instance.returnDbMap()         
            def sql = Sql.newInstance( db.url, db.user, db.password, db.driver )
            log.info "Starting clamAV"
            def clamAV = ClamAvClientHolder.getClamAvClient()
            if ( ClamAvClientHolder.checkClam( clamAV ) ) {
                log.info "Starting InboundSpoolWorker"
                def isw = new InboundSpoolWorker()
                log.info "About to run CLAM"
                isw.runClam( sql, clamAV )
                log.info "About to call moveCleanMessage"
                isw.moveCleanMessages( sql )
                log.info "about to call deleteTransferredMessages"
                isw.deleteTransferredMessages( sql )
                log.info "About to construct OutboundSpoolWorker"
                try {
                    def osw = new OutboundSpoolWorker()
                    log.info "about to call osw.runClam( sql, clamAV )"
                    osw.runClam( sql, clamAV )
                    log.info "here is message.serverList: ${message.serverList}"
                    osw.findInvalidUsers( sql, message.serverList )
                    osw.deleteInvalidUserMessages( sql )
                    
                    log.info "About to call osw.deliverMessages( sql, ${message.serverList}, ${message.port} )"
                    osw.deliverMessages( sql, message.serverList, message.port )
                    log.info "About to call osw.deleteTransferredMessages( sql )"
                    osw.deleteTransferredMessages( sql )
                    log.info "osw.deleteTransferredMessages( sql )"
                } catch ( Exception e ) {
                    log.error "Exception: ", e
                } finally {
                    sql.close()
                }
            }
        }
    }
    
    void onMessage( Object message ) {
        log.info "Caught generic message which is type ${message.getClass().name()}" 
    }
}

