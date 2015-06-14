package info.shelfunit.mail

import java.net.ServerSocket
import info.shelfunit.socket.BinarySMTPSocketWorker
import groovy.util.logging.Slf4j 

@Slf4j
class BinarySMTPServer {
    
    private String serverName
    
    def BinarySMTPServer( def argServer ) {
        serverName = argServer
        log.info "the server is ${argServer}, now it's ${serverName}"
    }
    
    def doStuff( port ) {
        def server = new ServerSocket( port )
 
        while ( true ) { 
            server.accept {  socket ->
                log.info "processing new connection..."
                socket.withStreams { input, output ->
                    log.info "input is a ${input.class.name}, output is a ${output.class.name}, the server is ${serverName}"
                    BinarySMTPSocketWorker sSockW = new BinarySMTPSocketWorker( input, output, serverName )
                    sSockW.doWork(  )
                }
                log.info "processing/thread complete......................"
            }
        }

    }
}

