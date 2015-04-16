package info.shelfunit.socket

// import java.net.SocketInputStream
// import java.net.SocketOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.BufferedReader

class SMTPSocketWorker {
    
    // private SocketInputStream input
    // private SocketOutputStream output

    private InputStream input
    private OutputStream output
    
    SMTPSocketWorker( argIn, argOut ) {
        input = argIn
        output = argOut
    }
    
    def doWork() {
        String sCurrentLine
        println "beginning doWork"
        println "name of current thread: ${Thread.currentThread().getName()}, and it is number ${Thread.currentThread().getId()}"

        println "input is a ${input.class.name}"
                    
        println "available: ${input.available()}"
        def reader = input.newReader()
        println "reader is a ${reader.class.name}"
        // def buffer = reader.readLine()
        /*
	while ( ( sCurrentLine = reader.readLine() ) != null ) {
            println( sCurrentLine );
        }
        */
        // println "server received: $buffer"
        println "can reader still be read before output? ${reader.ready()}"
        def now = new Date()
        output << "220 foo.com Simple Mail Transfer Service Ready\r\n"
	// output.write( "220 foo.com Simple Mail Transfer Service Ready" )
	// output.flush() // no need for flush
	println "can reader still be read after output? ${reader.ready()}"
	def buffer = reader.readLine()
	println "Here is the buffer: ${buffer}"
	/*
        reader = input.newReader()
        while ( ( sCurrentLine = reader.readLine() ) != null ) {
            println( sCurrentLine );
        }
	*/
	println "ending doWork"
    }
}

