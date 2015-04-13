package info.shelfunit.socket;

// from https://systembash.com/a-simple-java-tcp-server-and-tcp-client/

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

class JTCPServer {
    public static void main( String args[] ) throws Exception {
        String clientSentence;
        String capitalizedSentence;
        ServerSocket welcomeSocket = new ServerSocket( Integer.parseInt( args[ 0 ] )  );
        
        while ( true ) {
            Socket connectionSocket = welcomeSocket.accept();
            BufferedReader inFromClient =
               new BufferedReader( new InputStreamReader( connectionSocket.getInputStream() ) );
            System.out.println( "Got a connection" );
            DataOutputStream outToClient = new DataOutputStream( connectionSocket.getOutputStream() );
            clientSentence = inFromClient.readLine();
            System.out.println( "Received: " + clientSentence );
            capitalizedSentence = clientSentence.toUpperCase() + '\n';
            outToClient.writeBytes( capitalizedSentence );
       }
    }
}

