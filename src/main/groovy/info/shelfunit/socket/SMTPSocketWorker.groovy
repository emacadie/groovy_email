package info.shelfunit.socket

import java.io.InputStream
import java.io.OutputStream
import java.io.BufferedReader

class SMTPSocketWorker {

    private InputStream input
    private OutputStream output
	private String domain
    private String theResponse
    private String serverName

    SMTPSocketWorker( argIn, argOut, argServerName ) {
        input = argIn
        output = argOut
        serverName = argServerName
        println "server name is ${serverName}"
    }
    
	// make sure private fields are truly private
	def setInput( arg ) {}
	def setOutput( arg ) {}
	def setDomain( arg ) {}
	def setTheResponse( arg ) {}

    def doWork() {
        String sCurrentLine
        println "beginning doWork, input is a ${input.class.name}"
        println "name of current thread: ${Thread.currentThread().getName()}, and it is number ${Thread.currentThread().getId()}"            
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
        output << "220 ${serverName} Simple Mail Transfer Service Ready\r\n"
        
        println "can reader still be read after output? ${reader.ready()}"
        def buffer = reader.readLine()
        println "Here is the buffer: ${buffer}"
        theResponse = this.handleMessage( buffer )
        println "theResponse is a ${theResponse.class.name}"
        output << theResponse
        println "sent response"
        buffer = input.newReader().readLine()
        println "buffer after responding to ELHO: ${buffer}"
        println "ending doWork"
    }

	def handleMessage( theMessage ) {
		if ( theMessage.startsWith( 'EHLO' ) ) {
			domain = theMessage.replaceFirst( 'EHLO ', '' )
			println "Here is the domain: ${domain}"
			theResponse = "250-Hello ${domain}\n"
		    theResponse += "250 HELP\r\n"
			println "Here is the response:\n${theResponse}"
		} else if ( theMessage.startsWith( 'HELO' ) ) {
			domain = theMessage.replaceFirst( 'HELO ', '' )
			println "Here is the domain: ${domain}"
			theResponse = "250 Hello ${domain}\r\n"
		}
		theResponse
	}
}

