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
                println "Here is theLine before read: ${theLine}"
                try {
                theLine = reader?.readLine()
                } catch ( Exception ex ) {
                    println "exception: ${ex.printMessage()}"
                    ex.printStackTrace()
                }
                println "Here is theLine after read: ${theLine}"
            }
            println "Here is theLine: ${theLine}"
            if ( theLine.endsWith( "\r\n" ) ) { println "line ends with CRLF" }
            println "Done iterating"

            def buffer = "hello" // sBuffer.toString()
            println "server received: ${buffer}"
            now = new Date()
            output << "echo-response($now): " + buffer + "\n"
        }
        println "processing/thread complete------------------------"
    }
}
