package info.shelfunit.smtp

import groovy.util.logging.Slf4j 

@Slf4j
final class SMTPRunnerMessage {
    
    final String serverList;

    SMTPRunnerMessage( final String argServerList ) {
        serverList = argServerList
    }
}

