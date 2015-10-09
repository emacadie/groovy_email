package info.shelfunit.postoffice

import groovy.util.logging.Slf4j 

import groovyx.gpars.actor.DynamicDispatchActor

@Slf4j
class PostOfficeActor extends DynamicDispatchActor {
    
    void onMessage( PostOfficeRunnerMessage message ) {
        PostOfficeServer poServer = new PostOfficeServer( serverList )
        poServer.doStuff( config.smtp.server.port.toInteger() )
    }
}

