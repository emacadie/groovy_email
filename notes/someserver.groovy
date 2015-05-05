import java.net.ServerSocket
def server = new ServerSocket( 4444 )
 
while ( true ) { 
    server.accept {  socket ->
        println "processing new connection..."
        socket.withStreams { input, output ->
            println "name of current thread: ${Thread.currentThread().getName()}, and it is number ${Thread.currentThread().getId()}"
            println "input is a ${input.class.name}"
            println "output is a ${output.class.name}"
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
byte[] array = [0, 0, 0, 0, 0]

// second attempt
import java.net.ServerSocket
def server = new ServerSocket( 25 )

while ( true ) {
    server.accept { socket ->
        println "processing new connection..."
        socket.withStreams { input, output ->
            def reader = input.newReader()
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

// third attempt
import java.net.ServerSocket
def server = new ServerSocket( 25 )
 
while ( true ) { 
    server.accept {  socket ->
        println "processing new connection..."
        socket.withStreams { input, output ->
            println "input is a ${input.class.name}"
            def x = [  ] as byte[]
            def numBytes = input.read( x )
            println "read ${numBytes} bytes"
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
byte[] bArray = [0, 0, 0, 0, 0]
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