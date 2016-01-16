package info.shelfunit.smtp

import groovy.util.logging.Slf4j 

import groovyx.gpars.actor.DynamicDispatchActor

import info.shelfunit.mail.ConfigHolder

@Slf4j
class SMTPActor extends DynamicDispatchActor {
    
    SMTPServer smtp = null
    
    void onMessage( SMTPRunnerMessage message ) {
        log.info "About to start the server"
        log.info "here is the serverList: ${message.serverList} and it's a ${message.serverList.getClass().getName()}"
        smtp = new SMTPServer( message.serverList )
        log.info "About to get config"
        // def config = ConfigHolder.instance.getConfObject()
        // def config = ConfigHolder.instance.confObject
        // ConfigHolder.instance.setConfObject( args[ 0 ] )
        // def config = ConfigHolder.instance.getConfObject()
        log.info "About to call doStuff with port ${message.port} and it's a ${message.port.getClass().getName()}"
        smtp.doStuff( message.port )
    }
    
    void onMessage( Object message ) {
        log.info "Caught generic message which is type ${message.getClass().name()}" 
    }
}

