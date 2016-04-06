package info.shelfunit.spool

import groovy.util.logging.Slf4j 

@Slf4j
class SpoolRunnerMessage {
    
    final List serverList
    final int port

    SpoolRunnerMessage( final List argServerList, final int argPort ) {
        log.info "Constructing a SpoolRunnerMessage"
        serverList = argServerList
        port = argPort
    }

}

