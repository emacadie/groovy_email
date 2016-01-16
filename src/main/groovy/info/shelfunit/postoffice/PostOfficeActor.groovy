package info.shelfunit.postoffice

import groovy.util.logging.Slf4j 

import groovyx.gpars.actor.DynamicDispatchActor

import info.shelfunit.mail.ConfigHolder

@Slf4j
class PostOfficeActor extends DynamicDispatchActor {
    
    PostOfficeServer poServer = null
    
    void onMessage( PostOfficeRunnerMessage message ) {
        log.info "About to start the server"
        log.info "here is the serverList: ${message.serverList}"
        poServer = new PostOfficeServer( message.serverList )
        def config = ConfigHolder.instance.getConfObject()
        log.info "About to call doStuff with ${config.postoffice.server.port.toInteger() }"
        poServer.doStuff( config.postoffice.server.port.toInteger()  )
    }
    
    void onMessage( Object message ) {
        log.info "Caught generic message which is type ${message.getClass().name()}" 
    }
}

