package info.shelfunit.mail

import java.net.ServerSocket
import info.shelfunit.socket.SMTPSocketWorker

class SMTPServer {
    
    private String serverName
    
    def SMTPServer( def argServer ) {
        serverName = argServer
        println "the server is ${argServer}, now it's ${serverName}"
    }
    
    def doStuff( port ) {
        def server = new ServerSocket( port )
 
        while ( true ) { 
            server.accept {  socket ->
                println "processing new connection..."
                socket.withStreams { input, output ->
                    println "input is a ${input.class.name}, output is a ${output.class.name}, the server is ${serverName}"
                    SMTPSocketWorker sSockW = new SMTPSocketWorker( input, output, serverName )
                    sSockW.doWork(  )
                }
                println "processing/thread complete......................"
            }
        }

    }
}

