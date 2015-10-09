package info.shelfunit.postoffice

import groovy.util.logging.Slf4j 

@Slf4j
final class PostOfficeRunnerMessage {
    
    final String serverList;

    PostOfficeRunnerMessage( final String argServerList ) {
        serverList = argServerList
    }
}

