package info.shelfunit.smtp

import groovy.util.logging.Slf4j 

@Slf4j
final class SMTPRunnerMessage {
    
    final List serverList
    final int port

    SMTPRunnerMessage( final List argServerList, final int argPort ) {
        serverList = argServerList
        port = argPort
    }
}

