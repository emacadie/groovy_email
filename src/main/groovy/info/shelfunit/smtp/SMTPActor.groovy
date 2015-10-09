package info.shelfunit.smtp

import groovy.util.logging.Slf4j 

import groovyx.gpars.actor.DynamicDispatchActor

@Slf4j
class SMTPActor extends DynamicDispatchActor {
    
    void onMessage( SMTPRunnerMessage message ) {
        SMTPServer smtp = new SMTPServer( serverList )
        smtp.doStuff( config.smtp.server.port.toInteger() )
    }
}

