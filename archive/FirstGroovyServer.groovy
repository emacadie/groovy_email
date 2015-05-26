package info.shelfunit.socket

import java.net.ServerSocket

class FirstGroovyServer {
    
    def doStuff( port ) {
        def server = new ServerSocket( port )
 
        while ( true ) { 
            server.accept {  socket ->
                println "processing new connection..."
                socket.withStreams { input, output ->
                    String sCurrentLine
                
                    println "input is a ${input.class.name}"
                    
                    println "available: ${input.available()}"
                    def reader = input.newReader()
                    println "reader is a ${reader.class.name}"
                    // def buffer = reader.readLine()
                    while ( ( sCurrentLine = reader.readLine() ) != null ) {
                        println( sCurrentLine );
                    }
        
                    println "server received: $buffer"
                    println "can reader still be read? ${reader.ready()}"
                    now = new Date()
                    output << "echo-response($now): " + buffer + "\n"
                }
                println "processing/thread complete."
            }
        }

    }
    
    static main( args ) {
        def fGS = new FirstGroovyServer()
        fGS.doStuff( Integer.parseInt( args[ 0 ] ) )
    }
    

}

