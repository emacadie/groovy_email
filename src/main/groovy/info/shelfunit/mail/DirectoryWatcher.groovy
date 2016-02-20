package info.shelfunit.mail

import groovy.lang.Singleton

import groovy.util.logging.Slf4j 

// import java.io.File;
// import java.io.IOException;
// import java.net.URI;
// import java.net.URISyntaxException;
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.nio.file.WatchService

// based on code from https://gist.github.com/djangofan/2939268

@Slf4j
@Singleton( property = 'dirWatcher', strict = false )
class DirectoryWatcher {
    static Path tmpPath
    static WatchService watchService
    static boolean anyChange
    // DirectoryWatcher( ) { }
    
    def static init( String dirPath ) {
        tmpPath = Paths.get( dirPath )
        watchService = FileSystems.getDefault().newWatchService()
        anyChange = false
        log.info "Starting ${this.class.name}"
    }
    
    def static watch() {
        
        // Watching the /tmp/nio/ directory for MODIFY and DELETE operations
        tmpPath.register( watchService, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE )

        for ( ; ; ) {
            WatchKey key = watchService.take()
        
            // Poll all the events queued for the key
            // orig
            // for ( WatchEvent<?> event: key.pollEvents() ) {
            for ( WatchEvent event: key.pollEvents() ) {
                WatchEvent.Kind kind = event.kind()
                switch ( kind.name( ) ) {
                    case "ENTRY_MODIFY":
                        println( "println Modified: " + event.context() )
                        log.info "log Modified: " + event.context() 
                        break
                    case "ENTRY_DELETE":
                        println( "Delete: " + event.context() )
                        log.info "Delete: " + event.context()
                        break
                }
            }
            // reset is invoked to put the key back to ready state
            boolean valid = key.reset()
            // If the key is invalid, just exit.
            if ( !valid ) {
                break
            }
        }
        
    } // end method watch
}

