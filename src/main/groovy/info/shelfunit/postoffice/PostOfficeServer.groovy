package info.shelfunit.postoffice

import java.net.ServerSocket
// import info.shelfunit.socket.ModularSMTPSocketWorker
import groovy.util.logging.Slf4j 

@Slf4j
class PostOfficeServer {
    
    def serverList = []
    
    PostOfficeServer( def argServer ) {
        serverList = argServer
        log.info "the server is ${argServer}"
    }
    
    def doStuff( port ) {
        def server = new ServerSocket( port )
 
        while ( true ) { 
            server.accept {  socket ->
                log.info "processing new connection..."
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
