// from http://programmingitch.blogspot.com/2010/04/groovy-sockets-example.html
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
def server = new ServerSocket( 4444 )

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
def server = new ServerSocket( 4444 )
 
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
// trying again
import java.net.ServerSocket
def server = new ServerSocket( 4444 )
 
while ( true ) {
    server.accept { socket ->
        println "processing new connection..."
        socket.withStreams { input, output ->
            println "input is a ${input.getClass().getName()}"
            // buffer = reader.readLine()
            println "About to try reading buffer"
            // def buffer = input.newReader().readLine() // okay
            // def buffer = input.newReader().readLines() // no good
            // def buffer = input.newReader().getText() // no good
            def sBuffer = new StringBuffer()
            def holdLine
            /*
            def reader = input.newReader()
            println "Reader is a ${reader.getClass().getName()}"
            def iter = reader.iterator()
            while ( iter.hasNext() ) {
                println "Here is the next element: ${iter.next()}"
                println "About to get next, do we have next? "
                // println "iter is a ${iter?.getClass()?.getName()}"
                // if ( iter.hasNext() == null ) { println "All done" }
                // println " iter.hasNext() : ${ iter.hasNext() }"
            }
            */
            def strBuff = new StringBuffer()
            def byte[] tmp = [ ]
            println "input.available(): ${input.available()}"
            int i = 100
            while ( i > 0 ) {
                 i = input.read(tmp, 0, 1);
                 if ( i < 0 )
                      break;
                 strBuff.append(new String(tmp, 0, i));
            }
            // reader.close()
            println "Done iterating"
            /*
            while ( ( holdLine = reader.readLine() ) ) {
		        println "holdLine: ${holdLine}"
		        // sBuffer << holdLine
	        } // does not break out of loop
	        */
	        def buffer = "hello" // sBuffer.toString()
            println "server received: ${buffer}"
            now = new Date()
            output << "echo-response($now): " + buffer + "\n"
        }
        println "processing/thread complete------------------------"
    }
}
// and the client
s = new Socket( "localhost", 4444 );
s.withStreams { input, output ->
  output << "first line \necho testing ...\nanother line\n"
  buffer = input.newReader().readLine()
  println "response = $buffer"
}
////
// trying again
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
            def cc = reader.eachLine {
                println "Here is the next element: ${it}"

            }
            println "Done iterating"
            /*
            while ( ( holdLine = reader.readLine() ) ) {
		        println "holdLine: ${holdLine}"
		        // sBuffer << holdLine
	        } // does not break out of loop
	        */
	        def buffer = "hello" // sBuffer.toString()
            println "server received: ${buffer}"
            now = new Date()
            output << "echo-response($now): " + buffer + "\n"
        }
        println "processing/thread complete------------------------"
    }
}
// and the client
s = new Socket( "localhost", 4444 );
s.withStreams { input, output ->
  output << "first line \necho testing ...\nanother line\n"
  buffer = input.newReader().readLine()
  println "response = $buffer"
}
//
// trying again
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
            println "Done iterating"

	        def buffer = "hello" // sBuffer.toString()
            println "server received: ${buffer}"
            now = new Date()
            output << "echo-response($now): " + buffer + "\n"
        }
        println "processing/thread complete------------------------"
    }
}
// and the client
s = new Socket( "localhost", 4444 );
s.withStreams { input, output ->
  output << "fdddd \necho testing ...\nanother line\nXX\n"
  buffer = input.newReader().readLine()
  println "response = $buffer"
}
//
// trying again
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
            while ( ( holdLine = reader.readLine() ) ) {
                println "holdLine: ${holdLine}"
                if ( holdLine == "XX\n" ) {
                    reader.close()
                    // break 
                }
            } 

	        def buffer = "hello" // sBuffer.toString()
            println "server received: ${buffer}"
            now = new Date()
            output << "echo-response($now): " + buffer + "\n"
        }
        println "processing/thread complete------------------------"
    }
}
// and the client
s = new Socket( "localhost", 4444 );
s.withStreams { input, output ->
  output << "fdddd \necho testing ...\nanother line\nXX\n"
  buffer = input.newReader().readLine()
  println "response = $buffer"
}
// 
// I think I have something I can work with: Gotta send "XX\n"
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
            println "Done iterating"

            def buffer = "hello" // sBuffer.toString()
            println "server received: ${buffer}"
            now = new Date()
            output << "echo-response($now): " + buffer + "\n"
        }
        println "processing/thread complete------------------------"
    }
}
${new Date().toString()}
s = new Socket( "localhost", 4444 );
s.withStreams { input, output ->
  output << "${new Date().toString()} yyudd \nechoghg  testing ...\nanothergh  line\nXX\r\n"
  buffer = input.newReader().readLine()
  println "response = $buffer"
}
