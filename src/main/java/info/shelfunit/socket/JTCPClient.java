package info.shelfunit.socket;

// from https://systembash.com/a-simple-java-tcp-server-and-tcp-client/

import java.io.*;
import java.net.Socket;

class JTCPClient {
    public static void main( String args[] ) throws Exception {
        String sentence;
        String modifiedSentence;
        BufferedReader inFromUser = new BufferedReader( new InputStreamReader( System.in ) );
        Socket clientSocket = new Socket( "localhost", Integer.parseInt( args[ 0 ] ) );
        DataOutputStream outToServer = new DataOutputStream( clientSocket.getOutputStream() );
        BufferedReader inFromServer = new BufferedReader( new InputStreamReader( clientSocket.getInputStream() ) );
        sentence = inFromUser.readLine();
        outToServer.writeBytes( sentence + '\n' );
        modifiedSentence = inFromServer.readLine();
        System.out.println( "FROM SERVER: " + modifiedSentence );
        clientSocket.close();
    }
}


