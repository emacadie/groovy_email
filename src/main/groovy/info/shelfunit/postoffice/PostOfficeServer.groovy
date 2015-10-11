package info.shelfunit.postoffice

import java.net.ServerSocket
import groovy.util.logging.Slf4j 

@Slf4j
class PostOfficeServer {
    
    def serverList = []
    
    PostOfficeServer( def argServer ) {
        log.info "Starting server, list is ${argServer}"
        serverList = argServer
        log.info "server starting"
    }
    
    def doStuff( port ) {
        log.info "In doStuff and port ${port} is a ${port.getClass().getName()}"
        def server = new ServerSocket( port )
 
        while ( true ) { 
            server.accept {  socket ->
                log.info "processing new connection..."
                socket.setSoTimeout( 1000 * 60 * 10 ) // RFC 5321 states timeouts should be 2-10 minutes
                socket.withStreams { input, output ->
                    log.info "input is a ${input.class.name}, output is a ${output.class.name}, the server is ${serverList}"
                    ModularPostOfficeSocketWorker sSockW = new ModularPostOfficeSocketWorker( input, output, serverList )
                    sSockW.doWork(  )
                }
                log.info "processing/thread complete......................"
            }
        }

    }
}

