package info.shelfunit.mail

import java.net.ServerSocket
import info.shelfunit.socket.SMTPSocketWorker

class SMTPServer {
    
    private String server
    
    def SMTPServer( def argServer ) {
        server = argServer
        println "the server is ${argServer}"
    }
    
    def doStuff( port ) {
        def server = new ServerSocket( port )
 
        while ( true ) { 
            server.accept {  socket ->
                println "processing new connection..."
                socket.withStreams { input, output ->
                println "input is a ${input.class.name}"
                    println "output is a ${output.class.name}"
                    SMTPSocketWorker sSockW = new SMTPSocketWorker( input, output, server )
                    sSockW.doWork(  )

                }
                println "processing/thread complete."
            }
        }

    }
}

