import java.net.ServerSocket
def server = new ServerSocket( 4444 )
 
while ( true ) {
    server.accept { socket ->
        println "processing new connection..."
        socket.withStreams { input, output ->
            
            println "About to try reading buffer, input is a ${input.getClass().getName()}"
            // def buffer = input.newReader().readLine() // okay
            // def byte[] byteArray = [ ]
            def a = byte[  ] // was def a = new byte[ 1024 ] 
            // def numBytes = input.read( a )
            def numBytes = input.read()
            println "numBytes: ${numBytes} and it's a ${numBytes.getClass().getName()}"
            def holder = new String( numBytes ) // def holder = new String( a )
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

/////////////////////////////////////////////////
import java.net.ServerSocket
def server = new ServerSocket( 4444 )
 StringBuffer.metaClass.endsWith = { endString ->
            if ( delegate.substring( ( delegate.length() - endString.length() ), delegate.length() ).equals( endString ) ) {
                return true
            } else {
                return false
            }   
        }
        
while ( true ) {
    server.accept { socket ->
        println "processing new connection..."
        socket.withStreams { input, output ->
            def sBuff = new StringBuffer()
            println "About to try reading buffer, input is a ${input.getClass().getName()}"

            def a = byte[  ] // was def a = new byte[ 1024 ] 
            
            
            input.eachByte {
                println "here is the byte: ${it as char}"
                sBuff << (it as char)
                if ( sBuff.length() > 20 ) { println "sBuff bigger than 20: ${sBuff}" }
            }
            println "numBytes: ${numBytes} and it's a ${numBytes.getClass().getName()}"
            def holder = new String( numBytes ) // def holder = new String( a )
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
8410411732741171103248523250495851495851543267688432504849531032121121117100100321010199104111103104103323211610111511610511010332464646109711011111610410111410310432321081051101011088881310

