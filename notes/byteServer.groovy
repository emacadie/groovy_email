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
StringBuffer.metaClass.endsWith = { eString ->
    if ( delegate.length() < eString.length() ) {
        return false
    } else if ( delegate.substring( ( delegate.length() - eString.length() ), delegate.length() ).equals( eString ) ) {
        return true
    } else {
        return false
    }   
}
StringBuffer.metaClass.clear = { ->
    delegate.delete( 0, delegate.length() )
}
        
while ( true ) {
    server.accept { socket ->
        println "processing new connection..."
        socket.withStreams { input, output ->
            def sBuff = new StringBuffer()
            println "About to try reading buffer, input is a ${input.getClass().getName()}"

            def a = byte[  ] // was def a = new byte[ 1024 ] 
            def theByte
            while ( ( !sBuff.endsWith( "\r\n" ) ) ) {
                theByte = input.read()
                println "theByte as char: ${theByte as char}"
                sBuff << ( theByte as char )
                // if ( sBuff.length() > 20 ) { println "sBuff bigger than 20: ${sBuff}" }
            }

            def holder = new String( sBuff.toString() ) // def holder = new String( a )
            println "here is holder: ${holder}"
            def buffer 
            
            println "Done iterating"

            buffer = "hello" // sBuffer.toString()
            println "server received: ${buffer}"
            now = new Date()
            output << "echo-response($now): " + buffer + "\n"
            ////////////////////////////////////////////////
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
/////////////////////////////////////////////////
import java.net.ServerSocket
def server = new ServerSocket( 4444 )
StringBuffer.metaClass.endsWith = { eString ->
    if ( delegate.length() < eString.length() ) {
        return false
    } else if ( delegate.substring( ( delegate.length() - eString.length() ), delegate.length() ).equals( eString ) ) {
        return true
    } else {
        return false
    }   
}
StringBuffer.metaClass.clear = { ->
    delegate.delete( 0, delegate.length() )
}
        
while ( true ) {
    server.accept { socket ->
        println "processing new connection..."
        socket.withStreams { input, output ->
            def sBuff = new StringBuffer()
            println "About to try reading buffer, input is a ${input.getClass().getName()}"

            def a = byte[  ] // was def a = new byte[ 1024 ] 
            def theByte
            def holder
            def buffer 
            while ( ( !sBuff.endsWith( "\r\n" ) ) ) {
                theByte = input.read()
                // println "theByte as char: ${theByte as char}"
                sBuff << ( theByte as char )
                // if ( sBuff.length() > 20 ) { println "sBuff bigger than 20: ${sBuff}" }
            }

            holder = new String( sBuff.toString() ) // def holder = new String( a )
            println "here is holder: ${holder}"
            println "Done iterating"

            buffer = "hello" // sBuffer.toString()
            println "server received: ${buffer}"
            now = new Date()
            output << "echo-response($now) ${UUID.randomUUID().toString()}: " + buffer + "\n"
            ////////////////////////////////////////////////
            sBuff.clear()
            while ( ( !sBuff.endsWith( "\r\n" ) ) ) {
                theByte = input.read()
                // println "theByte as char: ${theByte as char}"
                sBuff << ( theByte as char )
                // if ( sBuff.length() > 20 ) { println "sBuff bigger than 20: ${sBuff}" }
            }

            holder = new String( sBuff.toString() ) // def holder = new String( a )
            println "here is holder: ${holder}"
            println "Done iterating"

            buffer = "hello" // sBuffer.toString()
            println "server received: ${buffer}"
            now = new Date()
            output << "echo-response($now): ${UUID.randomUUID().toString()} " + buffer + "\n"
            ////////////////////////////////////////////////
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
  //////
  output << "${new Date().toString()}\n This is the second message\nXX\r\n"
  buffer = input.newReader().readLine()
  println "response = $buffer"
}
8410411732741171103248523250495851495851543267688432504849531032121121117100100321010199104111103104103323211610111511610511010332464646109711011111610410111410310432321081051101011088881310

