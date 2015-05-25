import java.net.ServerSocket
def server = new ServerSocket( 4444 )
 
while ( true ) {
    server.accept { socket ->
        println "processing new connection..."
        socket.withStreams { input, output ->
            
            println "About to try reading buffer"
            // def buffer = input.newReader().readLine() // okay
            // def byte[] byteArray = [ ]
            def a = new byte[ 1024 ] 
            def numBytes = input.read( a )
            println "numBytes: ${numBytes}"
            def holder = new String( a )
            println "here is holder: ${holder}"
            def buffer 
            
            println "Done iterating"

            buffer = "hello" // sBuffer.toString()
            println "server received: ${buffer}"
            now = new Date()
            output << "echo-response($now): " + buffer + "\n"
        }
        println "processing/thread complete------------------------"
    }
}
//
${new Date().toString()}
s = new Socket( "localhost", 4444 );
s.withStreams { input, output ->
  output << "${new Date().toString()}\n yyudd \nechoghg  testing ...\nanothergh  line\nXX\r\n"
  buffer = input.newReader().readLine()
  println "response = $buffer"
}

