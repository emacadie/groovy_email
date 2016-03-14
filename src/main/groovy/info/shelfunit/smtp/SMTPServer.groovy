package info.shelfunit.smtp

import java.net.ServerSocket
import groovy.util.logging.Slf4j 

@Slf4j
class SMTPServer {
    
    def serverList = []
    
    def SMTPServer( def argServer ) {
        log.info "the server list is ${argServer} and it's a ${argServer.getClass().getName()}"
        serverList = argServer
        log.info "the server is starting"
    }
    
    def doStuff( def port ) {
        log.info "In doStuff and port ${port} is a ${port.getClass().getName()}"
        def server = new ServerSocket( port )
 
        while ( true ) { 
            sleep( 5.seconds() )
            log.info "done sleeping in SMTP"
            server.accept {  socket ->
                log.info "processing new connection..."
                socket.setSoTimeout( 10.minutes() ) // RFC 5321 states timeouts should be 2-10 minutes
                socket.withStreams { input, output ->
                    log.info "input is a ${input.class.name}, output is a ${output.class.name}, the server is ${serverList}"
                    log.info "socket address is ${socket.getInetAddress().toString}, its name is  ${socket.getInetAddress().getCanonicalHostName()}"
                    ModularSMTPSocketWorker sSockW
                    try {
                        sSockW = new ModularSMTPSocketWorker( input, output, serverList )
                        sSockW.doWork(  )
                    } finally {
                        sSockW.cleanup()
                    }
                }
                log.info "processing/thread complete......................"
            }
        }

    }
}

