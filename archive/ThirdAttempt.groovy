package info.shelfunit.socket

import java.net.ServerSocket

class ThirdAttempt {
    
    def doStuff( port ) {
        def server = new ServerSocket( port )
 
        while ( true ) { 
            server.accept {  socket ->
                println "processing new connection..."
                socket.withStreams { input, output ->
                    println "input is a ${input.class.name}"
                    println input.metaClass.methods*.name.sort().unique() 
                    def x = [  ] as byte[]
                    def numBytes = input.read( x )
                    println "read ${numBytes} bytes"
                    println "Here is x: ${x}"
                    // def reader = input.newReader()
                    // println "reader is a ${reader.class.name}"
                    // def buffer = reader.readLine()
                    // println "server received: $buffer"
                    // println "can reader still be read? ${reader.ready()}"
                    now = new Date()
                    output << "echo-response($now): " + x + "\n"
                }
                println "processing/thread complete."
            }
        }

    }
    
    static main( args ) {
        def ta = new ThirdAttempt()
        ta.doStuff( Integer.parseInt( args[ 0 ] ) )
    }
}

