package info.shelfunit.mail

import java.net.ServerSocket
import info.shelfunit.socket.SMTPSocketWorker

class BnarySMTPServer {
    
    private String serverName
    
    def BinarySMTPServer( def argServer ) {
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
                    BinarySMTPSocketWorker sSockW = new BinarySMTPSocketWorker( input, output, serverName )
                    sSockW.doWork(  )
                }
                println "processing/thread complete......................"
            }
        }

    }
}

