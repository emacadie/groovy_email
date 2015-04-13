package info.shelfunit.socket

import java.net.SocketInputStream
import java.net.SocketOutputStream
import java.io.BufferedReader

class SMTPSocketWorker {
    
    SocketInputStream input
    SocketOutputStream output
    
    SMTPSocketWorker( argIn, argOut ) {
        input = argIn
        output = argOut
    }
    
    def doWork() {
        String sCurrentLine
        println "input is a ${input.class.name}"
                    
        println "available: ${input.available()}"
        def reader = input.newReader()
        println "reader is a ${reader.class.name}"
        // def buffer = reader.readLine()
        while ( ( sCurrentLine = reader.readLine() ) != null ) {
            println( sCurrentLine );
        }
        
        println "server received: $buffer"
        println "can reader still be read? ${reader.ready()}"
        now = new Date()
        output << "echo-response($now): " + buffer + "\n"
    }
}

