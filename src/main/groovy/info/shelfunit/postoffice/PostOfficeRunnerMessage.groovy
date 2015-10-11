package info.shelfunit.postoffice

import groovy.util.logging.Slf4j 

@Slf4j
final class PostOfficeRunnerMessage {
    
    final List serverList;

    PostOfficeRunnerMessage( final List argServerList ) {
        serverList = argServerList
    }
}

