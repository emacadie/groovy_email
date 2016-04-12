import java.net.InetAddress
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLServerSocketFactory

info.shelfunit.mail.meta.MetaProgrammer.runMetaProgramming()

def socket = new Socket( 'mx4.hotmail.com.', 25 )
println "here is socket: ${socket}"
def input = socket.getInputStream()
def output = socket.getOutputStream()
def newString 
def done = false
def commandList = []
def reader = input.newReader()
println "${reader.readLine()}"
output << "EHLO testmail.com\r\n"
println "About to read input line"
while ( isNot( done ) ) {
    newString = reader.readLine()
    if ( doesNot( newString.matches( ".*[a-z].*" ) ) ) {
        commandList << newString.allButFirstFour()
    }
    println "Here is newString: ${newString}"
    if ( newString.startsWith( '250 ' ) ) { 
        done = true 
    }
    println "Done: ${done}"
}
println "Here is commandList: ${commandList}"

output << "STARTTLS\r\n"

// Get the default SSLSocketFactory
// SSLSocketFactory sf = ( ( SSLSocketFactory ) javax.net.ssl.SSLSocketFactory.getDefault() );
SSLSocketFactory sf = (  javax.net.ssl.SSLSocketFactory.getDefault() );

// Wrap 'socket' from above in a SSL socket
InetSocketAddress remoteAddress = ( InetSocketAddress ) socket.getRemoteSocketAddress();
SSLSocket s = ( SSLSocket ) ( sf.createSocket( socket, remoteAddress.getHostName(), socket.getPort(), true ) );

// we are a client
s.setUseClientMode( true );

// allow all supported protocols and cipher suites
s.setEnabledProtocols( s.getSupportedProtocols() );
s.setEnabledCipherSuites( s.getSupportedCipherSuites() );

// and go!
s.startHandshake();

// continue communication on 'socket'
socket = s;
output << "EHLO testmail.com\r\n"
newString = reader.readLine()
output << "EHLO testmail.com\r\n"
done = false
while ( isNot( done ) ) {
    newString = reader.readLine()
    if ( doesNot( newString.matches( ".*[a-z].*" ) ) ) {
        commandList << newString.allButFirstFour()
    }
    println "Here is newString: ${newString}"
    if ( newString.startsWith( '250 ' ) ) { 
        done = true 
    }
    println "Done: ${done}"
}
println "Here is commandList: ${commandList}"
// ////////////////
import net.cogindo.ssl.TLSSocketFactory
import java.net.InetAddress
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLServerSocketFactory
try {
    info.shelfunit.mail.meta.MetaProgrammer.runMetaProgramming()
    
    SSLSocketFactory sf = new TLSSocketFactory( )
    SSLSocket sslsocket = (SSLSocket) sf.createSocket( 'gmail-smtp-in.l.google.com.', 25 );
    
    println "here is socket: ${socket}"
    def input = socket.getInputStream()
    def output = socket.getOutputStream()
    def newString 
    def done = false
    def commandList = []
    def reader = input.newReader()
    println "${reader.readLine()}"
    output << "EHLO testmail.com\r\n"
    println "About to read input line"
    while ( isNot( done ) ) {
        newString = reader.readLine()
        if ( doesNot( newString.matches( ".*[a-z].*" ) ) ) {
            commandList << newString.allButFirstFour()
        }
        println "Here is newString: ${newString}"
        if ( newString.startsWith( '250 ' ) ) { 
            done = true 
        }
        println "Done: ${done}"
    }
    println "Here is commandList: ${commandList}"
    
    
} catch ( Exception e ) {
    e.printStackTrace()
}



