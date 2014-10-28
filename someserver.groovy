import java.net.ServerSocket
def server = new ServerSocket( 4444 )
 
while ( true ) { 
    server.accept {  socket ->
        println "processing new connection..."
        socket.withStreams { input, output ->
            println "input is a ${input.class.name}"
            def reader = input.newReader()
            println "reader is a ${reader.class.name}"
            def buffer = reader.readLine()
            println "server received: $buffer"
            println "can reader still be read? ${reader.ready()}"
            now = new Date()
            output << "echo-response($now): " + buffer + "\n"
        }
        println "processing/thread complete."
    }
}
// input is a java.net.SocketInputStream
// reader is a java.io.BufferedReader
/*
To use:
s = new Socket("localhost", 4444);
s.withStreams { input, output ->
  output << "echo testing ...\n"
  buffer = input.newReader().readLine()
  println "response = $buffer"
}

s3 = new Socket("localhost", 4444);
s3.withStreams { input, output ->
  output << "Love those Taiwan lay-deez ...\n"
  buffer = input.newReader().readLine()
  println "response = $buffer"
}

 */