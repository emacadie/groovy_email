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
            def sqlObject = Sql.newInstance( db.url, db.user, db.password, db.driver )
            log.info "Starting clamAV"
            def clamAV = ClamAvClientHolder.getClamAvClient()
            if ( ClamAvClientHolder.checkClam( clamAV ) ) {
                log.info "Starting InboundSpoolWorker"
                def isw = new InboundSpoolWorker()
                log.info "About to run CLAM"
                isw.runClam( sqlObject, clamAV )
                log.info "About to call moveCleanMessage"
                isw.moveCleanMessages( sqlObject )
                log.info "about to call deleteTransferredMessages"
                isw.deleteTransferredMessages( sqlObject )
                log.info "About to construct OutboundSpoolWorker"
                try {
                    def osw = new OutboundSpoolWorker()
                    log.info "about to call osw.runClam( sqlObject, clamAV )"
                    osw.runClam( sqlObject, clamAV )
                    log.info "here is message.serverList: ${message.serverList}"
                    osw.findInvalidUsers( sqlObject, message.serverList )
                    osw.deleteInvalidUserMessages( sqlObject )
                    
                    log.info "About to call osw.deliverMessages( sqlObject, ${message.serverList}, ${message.port} )"
                    osw.deliverMessages( sqlObject, message.serverList, message.port )
                    log.info "About to call osw.deleteTransferredMessages( sqlObject )"
                    osw.deleteTransferredMessages( sqlObject )
                    log.info "osw.deleteTransferredMessages( sqlObject )"
                } catch ( Exception e ) {
                    log.error "Exception: ", e
                } finally {
                    sqlObject.close()
                }
            }
        }
    }
    
    void onMessage( Object message ) {
        log.info "Caught generic message which is type ${message.getClass().name()}" 
    }
}

