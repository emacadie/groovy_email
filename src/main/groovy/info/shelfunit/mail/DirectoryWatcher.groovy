package info.shelfunit.mail

import groovy.util.logging.Slf4j 

// import java.io.File;
// import java.io.IOException;
// import java.net.URI;
// import java.net.URISyntaxException;
// import java.nio.file.*;

import java.nio.file.Path
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.nio.file.WatchService

// based on code from https://gist.github.com/djangofan/2939268

@Slf4j
class DirectoryWatcher {
    Path tmpPath
    WatchService watchService
    boolean anyChange
    DirectoryWatcher( String dirPath ) {
        tmpPath = Paths.get( dirPath )
        watchService = FileSystems.getDefault().newWatchService()
        anyChange = false
    }
    
    def watch() {
        
        // Watching the /tmp/nio/ directory for MODIFY and DELETE operations
        tmpPath.register(
        watchService,
        StandardWatchEventKinds.ENTRY_MODIFY,
        StandardWatchEventKinds.ENTRY_DELETE);

        for ( ; ; ) {
            WatchKey key = watchService.take()
        
            //Poll all the events queued for the key
            for ( WatchEvent<?> event: key.pollEvents()){
                WatchEvent.Kind kind = event.kind()
                switch (kind.name()){
                    case "ENTRY_MODIFY":
                        System.out.println("Modified: "+event.context())
                        break
                    case "ENTRY_DELETE":
                        System.out.println("Delete: "+event.context())
                        break
                }
            }
            //reset is invoked to put the key back to ready state
            boolean valid = key.reset()
            //If the key is invalid, just exit.
            if ( !valid ) {
                break
            }
        }
        
    }
}

