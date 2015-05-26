package info.shelfunit.socket

import java.net.ServerSocket

class FourthAttempt {
    
    def doStuff( port ) {
        
        def server = new ServerSocket( port )
        
        while ( true ) { 
            server.accept {  socket ->
                println "processing new connection..."
                socket.withObjectStreams { input, output ->
                    println "input is a ${input.class.name}"
                    println input.metaClass.methods*.name.sort().unique() 
                    def arg1 = input.readObject()
                    def arg2 = output.readObject()
                    def x = [  ] as byte[]
                    // def numBytes = input.read(  )
                    // println "read ${numBytes} bytes"
                    // def reader = input.newReader()
                    // println "reader is a ${reader.class.name}"
                    // def buffer = reader.readLine()
                    // println "server received: $buffer"
                    // println "can reader still be read? ${reader.ready()}"
                    // now = new Date()
                    // output << "echo-response($now): " "\n"
                    println "done ${now} "
                }
                println "processing/thread complete."
            }
        }

    }
    
    static main( args ) {
        def fa = new FourthAttempt()
        fa.doStuff( Integer.parseInt( args[ 0 ] ) )
    }
}

