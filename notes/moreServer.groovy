import java.net.ServerSocket
def server = new ServerSocket( 4444 )
 
while ( true ) {
    server.accept { socket ->
        println "processing new connection..."
        socket.withStreams { input, output ->
            
            // buffer = reader.readLine()
            println "About to try reading buffer"
            // def buffer = input.newReader().readLine() // okay
            // def buffer = input.newReader().readLines() // no good
            // def buffer = input.newReader().getText() // no good
            def sBuffer = new StringBuffer()
            def holdLine
            def reader = input.newReader()
            def theLine = reader.readLine()
            println "First line: ${theLine}"
            while ( !theLine.startsWith( "XX" ) ) {
                println "Here is theLine before read: ${theLine}  and it's a ${theLine.getClass().getName()}"
                try {
                theLine = reader?.readLine()
                } catch ( Exception ex ) {
                    println "exception: ${ex.printMessage()}"
                    ex.printStackTrace()
                }
                println "Here is theLine after read: ${theLine}"
                if ( theLine.endsWith( "\r\n" ) ) { println "line ends with CRLF" 
                } else if ( theLine.endsWith( "\n" ) )  { println "line ends with LF" 
                } else if ( theLine.endsWith( "\r" ) )  { println "line ends with CR" 
                } else { println "line ends with something else" }
                if ( theLine.matches( ".*\n" ) ) { println "regex says line ends with LF" }
                
            }
            println "Here is theLine: ${theLine}"
            if ( theLine.endsWith( "\r\n" ) ) { println "line ends with CRLF" 
            } else if ( theLine.endsWith( "\r" ) )  { println "line ends with CR" }
            println "Done iterating"

            def buffer = "hello" // sBuffer.toString()
            println "server received: ${buffer}"
            now = new Date()
            output << "echo-response($now): " + buffer + "\n"
        }
        println "processing/thread complete------------------------"
    }
}

