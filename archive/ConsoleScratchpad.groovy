/*
------------------------+---------------------------------+---------------
 malcolm@shelfunit.info | eeee@wilson.net                 | ENTERED
 malcolm@shelfunit.info | eeee@gmail.com                  | ENTERED
 malcolm@shelfunit.info | eeee@gmail.com,eeee@yahoo.com   | ENTERED
 malcolm@shelfunit.info | eeee@gmail.com                  | ENTERED
 malcolm@shelfunit.info | eeee@yahoo.com                  | ENTERED
 malcolm@ShelfUnit.info | dan@chumbawumba                 | ENTERED
 malcolm@shelfunit.info | eeee@wilson.net                 | ENTERED
 malcolm@ShelfUnit.info | dan@chumbawumba                 | ENTERED
*/
// good one
// '''([\\w!#$%&’*+/=?`{|}~^-]+(?:\\.[\\w!#$%&’*+/=?`{|}~^-]+)*@((?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}))$(?x)'''
// '''([\\w!#$%&’*+/=?`{|}~^-]+(?:\\.[\\w!#$%&’*+/=?`{|}~^-]+)*@((?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}))$(?x)'''
/*
here is q[ 0 ][ 0 ]: eeee@yahoo.com
here is q[ 0 ][ 1 ]: eeee@yahoo.com
here is q[ 0 ][ 2 ]: eeee
here is q[ 0 ][ 3 ]: yahoo.com
*/
import info.shelfunit.mail.meta.MetaProgrammer

MetaProgrammer.runMetaProgramming()
def localPart  = '''(([\\w!#$%&’*+/=?`{|}~^-]+(?:\\.[\\w!#$%&’*+/=?`{|}~^-]+)*)@'''
def domainName = '''((?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}))'''
def regex = localPart + domainName  
def domainList = [ 'shelfunit.info', 'groovy-is-good.com' ]
def toListString = 'eeeeG@gmail.com,eeeeY@yahoo.com,ddddG@gmail.com,ddddY@yahoo.com,this.is@cnn.com' 
def toAddressList = toListString.split( ',' )
def outgoingMap = [:]
toAddressList.each { address ->
    println "Here is address: ${address}"
    def q = address =~ regex
    println "here is q[ 0 ][ 0 ]: ${q[ 0 ][ 0 ]}"
    println "here is q[ 0 ][ 1 ]: ${q[ 0 ][ 1 ]}"
    println "here is q[ 0 ][ 2 ]: ${q[ 0 ][ 2 ]}"
    println "here is q[ 0 ][ 3 ]: ${q[ 0 ][ 3 ]}"
    q.each { match ->
        match.eachWithIndex { group, n ->
            println "${n}, <$group>"
        }
    }
    outgoingMap.addDomainToOutboundMap( q.getDomainInOutboundSpool() )
    outgoingMap[ q.getDomainInOutboundSpool() ] << q.getUserInOutboundSpool()
    println "------"
}
outgoingMap.each { k, v ->
    println "++++"
    println "Key ${k} has value ${v}"
}

// //////////////////////// 
import org.xbill.DNS.Lookup
import org.xbill.DNS.MXRecord
import org.xbill.DNS.Record
import org.xbill.DNS.Type
def records = new Lookup( "gmail.com", Type.MX ).run()
def targetMap = [:]
def reverseMap = [:]
def priorityList = []
records.each { record ->
    MXRecord mx = ( MXRecord ) record
    println( "Host " + mx.target + " has preference " + mx.priority + " which is a " + mx.priority.getClass().name + " with TTL " + mx.ttl )
    targetMap[ mx.target ] = mx.priority
    priorityList << mx.priority
    reverseMap[ mx.priority ] = mx.target
}
println ""
targetMap.each { t, p ->
    println "target ${t} has priority ${p}"
}
println "reverse map"
reverseMap.each { k, v ->
    println "target ${v} has priority ${k}"
}
println "Minimum priority is ${priorityList.min()}"
println "So use server ${reverseMap[ priorityList.min() ]} which is a ${reverseMap[ priorityList.min() ].getClass().name}"
println "So use server ${reverseMap[ priorityList.min() ].toString()} "
println ""
////////////////////////////////////
def domainList = [ 'shelfunit.info', 'groovy-is-good.com' ]
def toListString = 'emacadie9@gmail.com,EMacAdie@yahoo.com'
def toAddressList = toListString.split( ',' )


import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
SSLSocketFactory f =  ( SSLSocketFactory ) SSLSocketFactory.getDefault()
println "Starting socket"
SSLSocket socket = (SSLSocket) f.createSocket( "gmail-smtp-in.l.google.com", 465 )
// SSLSocket socket = (SSLSocket) f.createSocket( "smtp.mail.yahoo.com", 465 )
println "Here is socket: ${socket}"
BufferedReader inp = new BufferedReader( new InputStreamReader( socket.getInputStream() ) ) ;
String x = inp.readLine();
System.out.println( x );
inp.close();

//////////////////////////////
info.shelfunit.mail.meta.MetaProgrammer.runMetaProgramming()
def socket = new Socket( 'mta7.am0.yahoodns.net', 25 )
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

// ////////////////////////
info.shelfunit.mail.meta.MetaProgrammer.runMetaProgramming()
def bString = '''250-mx.somedomain.com at your service, someotherdomain.com
250-SIZE 157286400
250-8BITMIME
250-STARTTLS
250-ENHANCEDSTATUSCODES
250-PIPELINING
250-CHUNKING
250 SMTPUTF8
220 2.0.0 Ready to start TLS
'''
byte[] data = bString.getBytes()
InputStream input = new ByteArrayInputStream( data )
def commandList = []
def newString 
def done = false
def reader = input.newReader()
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
////////////////////////////////////////////////////
import java.net.InetAddress
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLServerSocketFactory
import info.shelfunit.mail.SecurityTools

info.shelfunit.mail.meta.MetaProgrammer.runMetaProgramming()
def hostName = 'mta7.am0.yahoodns.net.'
def socket = new Socket( hostName, 25 )
println "here is socket: ${socket}"
println "socket is a ${socket.getClass().name}"
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

if ( commandList.contains( 'STARTTLS' ) ) {
    output << "STARTTLS\r\n"
    newString = reader.readLine()
    println "Here is newString: ${newString}"
    
    // Get the default SSLSocketFactory
    // SSLSocketFactory sf = ( ( SSLSocketFactory ) javax.net.ssl.SSLSocketFactory.getDefault() );
    SSLSocketFactory sf = (  javax.net.ssl.SSLSocketFactory.getDefault() );
    
    // Wrap 'socket' from above in a SSL socket
    InetSocketAddress remoteAddress = ( InetSocketAddress ) socket.getRemoteSocketAddress();
    SSLSocket s = ( SSLSocket ) ( sf.createSocket( socket, hostName, socket.getPort(), true ) );

    println "Here is s: ${s}"
    // we are a client
    s.setUseClientMode( true );
    println "Called s.setUseClientMode"
    
    // allow all supported protocols and cipher suites
    s.setEnabledProtocols( s.getSupportedProtocols() );
    println "called s.setEnabledProtocols( s.getSupportedProtocols() )"
    s.setEnabledCipherSuites( s.getSupportedCipherSuites() );
    println "called s.setEnabledCipherSuites( s.getSupportedCipherSuites() );"
    // and go!
    s.startHandshake();
    println "called s.startHandshake();"
    
    // continue communication on 'socket'
    socket = s;
    println "here is socket: ${socket}"
    println "socket is a ${socket.getClass().name}"
    output = socket.getOutputStream()
    input = socket.getInputStream()
    reader = input.newReader()
    // output << "EHLO testmail.com\r\n"
    // newString = reader.readLine()
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
} // if ( commandList.contains( 'STARTTLS' ) )
output << 'QUIT\r\n'
def reader = input.newReader()
println "${reader.readLine()}"
///////////////////////////////////
import java.net.InetAddress
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLServerSocketFactory

import javax.net.ssl.SSLContext
import javax.net.ssl.SSLException
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
// import java.io.*;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

char[] HEXDIGITS = "0123456789abcdef".toCharArray( )

    def toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder( bytes.length * 3 )
        char[] HEXDIGITS = "0123456789abcdef".toCharArray( )
        for (int b : bytes) {
            b &= 0xff;
            sb.append(HEXDIGITS[b >> 4] )
            sb.append(HEXDIGITS[b & 15] )
            sb.append(' ' )
        }
        return sb.toString( )
    }

    class SavingTrustManager implements X509TrustManager {

        private final X509TrustManager tm;
        private X509Certificate[] chain;

        SavingTrustManager(X509TrustManager tm) {
            this.tm = tm;
        }

        public X509Certificate[] getAcceptedIssuers() {
            throw new UnsupportedOperationException( )
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            throw new UnsupportedOperationException( )
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            this.chain = chain;
            tm.checkServerTrusted(chain, authType )
        }
    }

info.shelfunit.mail.meta.MetaProgrammer.runMetaProgramming()
def hostName = 'mta7.am0.yahoodns.net.'
def socket = new Socket( hostName, 25 )
println "here is socket: ${socket}"
println "socket is a ${socket.getClass().name}"
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

if ( commandList.contains( 'STARTTLS' ) ) {
    output << "STARTTLS\r\n"
    newString = reader.readLine()
    println "Here is newString: ${newString}"
    
    // Get the default SSLSocketFactory
    
            File file = new File( "jssecacerts" )
        if ( file.isFile() == false ) {
            char SEP = File.separatorChar;
            File dir = new File( System.getProperty( "java.home" ) + SEP
                    + "lib" + SEP + "security" )
            file = new File( dir, "jssecacerts" )
            if ( file.isFile() == false ) {
                file = new File( dir, "cacerts" )
            }
        }
        println( "Loading KeyStore " + file + "..." )
        InputStream ins = new FileInputStream( file )
        KeyStore ks = KeyStore.getInstance( KeyStore.getDefaultType() )
        def passphrase = 'changeit' as char[]
        ks.load( ins, passphrase )
        ins.close( )

        SSLContext context = SSLContext.getInstance( "TLS" )
        TrustManagerFactory tmf =
                TrustManagerFactory.getInstance( TrustManagerFactory.getDefaultAlgorithm() )
        tmf.init( ks )
        X509TrustManager defaultTrustManager = ( X509TrustManager ) tmf.getTrustManagers()[ 0 ];
        SavingTrustManager tm = new SavingTrustManager( defaultTrustManager )
        // context.init( null, new TrustManager[ tm ], null )
        context.init( null, [ tm ] as TrustManager[ ], null )
        SSLSocketFactory sf = context.getSocketFactory( )


    
    // Wrap 'socket' from above in a SSL socket
    InetSocketAddress remoteAddress = ( InetSocketAddress ) socket.getRemoteSocketAddress();
    SSLSocket s = ( SSLSocket ) ( sf.createSocket( socket, hostName, socket.getPort(), true ) );

    println "Here is s: ${s}"
    // we are a client
    s.setUseClientMode( true );
    println "Called s.setUseClientMode"
    
    // allow all supported protocols and cipher suites
    s.setEnabledProtocols( s.getSupportedProtocols() );
    println "called s.setEnabledProtocols( s.getSupportedProtocols() )"
    s.setEnabledCipherSuites( s.getSupportedCipherSuites() );
    println "called s.setEnabledCipherSuites( s.getSupportedCipherSuites() );"
    // and go!
    try {
    s.startHandshake();
    println "called s.startHandshake();"
    } catch ( Exception e ) {
        X509Certificate[] chain = tm.chain;
        if ( chain == null ) {
            println( "Could not obtain server certificate chain" )
            return;
        }

        BufferedReader breader =
                new BufferedReader( new InputStreamReader( System.in ) )

        println(  )
        println( "Server sent " + chain.length + " certificate(s):" )
        println(  )
        MessageDigest sha1 = MessageDigest.getInstance( "SHA1" )
        MessageDigest md5 = MessageDigest.getInstance( "MD5" )
        for ( int i = 0; i < chain.length; i++) {
            X509Certificate cert = chain[ i ];
            println(" " + ( i + 1 ) + " Subject " + cert.getSubjectDN() )
            println( "   Issuer  " + cert.getIssuerDN() )
            sha1.update( cert.getEncoded() )
            println( "   sha1    " + toHexString( sha1.digest() ) )
            md5.update( cert.getEncoded() )
            println( "   md5     " + toHexString( md5.digest() ) )
            println(  )
        }

        println( "Enter certificate to add to trusted keystore or 'q' to quit: [1]" )
        String line = '1'
        int k;
        try {
            k = ( line.length() == 0 ) ? 0 : Integer.parseInt( line ) - 1;
        } catch ( NumberFormatException ex ) {
            println( "KeyStore not changed" )
            // return;
        }

        X509Certificate cert = chain[ k ];
        String alias = hostName + "-" + ( k + 1 )
        ks.setCertificateEntry( alias, cert )

        OutputStream out = new FileOutputStream( "jssecacerts" )
        ks.store( out, passphrase )
        out.close( )

        println(  )
        println( cert )
        println(  )
        println("Added certificate to keystore 'jssecacerts' using alias '"
                        + alias + "'" )
    }
    // continue communication on 'socket'
    output << 'QUIT\r\n'
    /*
    socket = s;
    println "here is socket: ${socket}"
    println "socket is a ${socket.getClass().name}"
    output = socket.getOutputStream()
    input = socket.getInputStream()
    reader = input.newReader()
    // output << "EHLO testmail.com\r\n"
    // newString = reader.readLine()
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
    */
} // if ( commandList.contains( 'STARTTLS' ) )
output << 'QUIT\r\n'



