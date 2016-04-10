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

// //////////////////////
import org.xbill.DNS.Lookup
import org.xbill.DNS.MXRecord
import org.xbill.DNS.Record
import org.xbill.DNS.Type
Record [] records = new Lookup("gmail.com", Type.MX).run();
for (int i = 0; i < records.length; i++) {
	MXRecord mx = (MXRecord) records[i];
	System.out.println("Host " + mx.getTarget() + " has preference " + mx.getPriority());
}

import org.xbill.DNS.Lookup
import org.xbill.DNS.MXRecord
import org.xbill.DNS.Record
import org.xbill.DNS.Type
def records = new Lookup( "gmail.com", Type.MX ).run();
records.each { record ->
	MXRecord mx = ( MXRecord ) record
	System.out.println( "Host " + mx.getTarget() + " has preference " + mx.getPriority() );
}
// 
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
println "So use server ${reverseMap[ priorityList.min() ]}"
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
/*

def socket = new Socket( 'gmail-smtp-in.l.google.com', 25 )
println "here is socket: ${socket}"
def input = socket.getInputStream()
def output = socket.getOutputStream()
def reader = input.newReader()
println "About to read input line"
// inputLine = input.read()
def newString =  reader.readLine() 
println "Here is newString: ${newString}"
output << "EHLO qqq.com\r\n"
*/
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



