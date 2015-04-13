package info.shelfunit.socket

import java.net.ServerSocket

class SecondAttempt {
    
    def doStuff( port ) {
        def server = new ServerSocket( port )

        while ( true ) {
            server.accept { socket ->
                println "processing new connection..."
                socket.withStreams { input, output ->
                    println "input is a ${input.class.name}"
                    println input.metaClass.methods*.name.sort().unique() 	  
                    def reader = input.newReader()
                    println "got a new reader"
                    def buffer = null
                    while ( ( buffer = reader.readLine() ) != null ) {
                        //def buffer = reader.readLine()
                        println "server received: $buffer"
                        if ( buffer == "*bye*" ) {
                            println "exiting..."
                            System.exit( 0 )
                        } else {
                            output << "echo-response: " + buffer + "\n"
                        }
                    }
                }
                println "processing complete."
            }
        }

    }
    
    static main( args ) {
        def sa = new SecondAttempt()
        sa.doStuff( Integer.parseInt( args[ 0 ] ) )
    }
}

