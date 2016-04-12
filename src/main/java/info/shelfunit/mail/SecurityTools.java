package info.shelfunit.mail;

import sun.misc.BASE64Encoder;
import sun.security.provider.X509Factory;
import sun.security.x509.*;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.security.cert.*;
// import javax.security.cert.Certificate;
// import javax.security.cert.X509Certificate;
import java.security.cert.Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;

/** 
from http://stackoverflow.com/questions/25637039/detecting-ssl-connection-and-converting-socket-to-sslsocket
USAGE:
            while(isRunning)
            {
                BufferedSocket clientSocket = (BufferedSocket) serverSocket.accept();
 
                if(SecurityTools.isSSLPacket(clientSocket.getInputStream()))
                {
                    logger.debug(clientSocket.toString() + " handled as SSL client");
                    Socket clientSecureSocket = SecurityTools.convertToSecureSocket(clientSocket, String.format("cn=%s,ou=%s,o=%s,c=%s", cn, ou, o, c));
                    WebConnectionHandler.handleClient(clientSecureSocket);
                }
                else {
                    logger.debug(clientSocket.toString() + " handled as plain text client");
                    WebConnectionHandler.handleClient(clientSocket);
                }
            }

*/
public class SecurityTools {
 
    private static final File keyStoreFile = new File( "hexcast.jks" );
 
    public static X509Certificate generateCertificate(String dn, KeyPair pair, int days, String algorithm)
            throws GeneralSecurityException, IOException
    {
        PrivateKey privkey = pair.getPrivate();
        X509CertInfo info = new X509CertInfo();
        Date from = new Date();
        Date to = new Date( from.getTime() + days * 86400000l );
        CertificateValidity interval = new CertificateValidity( from, to );
        BigInteger sn = new BigInteger( 64, new SecureRandom() );
        X500Name owner = new X500Name( dn );
 
        info.set( X509CertInfo.VALIDITY, interval );
        info.set( X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber( sn ) );
        info.set( X509CertInfo.SUBJECT, new CertificateSubjectName( owner ) );
        info.set( X509CertInfo.ISSUER, new CertificateIssuerName( owner ) );
        info.set( X509CertInfo.KEY, new CertificateX509Key( pair.getPublic() ) );
        info.set( X509CertInfo.VERSION, new CertificateVersion( CertificateVersion.V3 ) );
        AlgorithmId algo = new AlgorithmId( AlgorithmId.md5WithRSAEncryption_oid );
        info.set( X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId( algo ) );
 
        // Sign the cert to identify the algorithm that's used.
        X509CertImpl cert = new X509CertImpl( info );
        cert.sign( privkey, algorithm );
 
        // Update the algorith, and resign.
        algo = ( AlgorithmId ) cert.get( X509CertImpl.SIG_ALG );
        info.set( CertificateAlgorithmId.NAME + "." + CertificateAlgorithmId.ALGORITHM, algo );
        cert = new X509CertImpl( info );
        cert.sign( privkey, algorithm );
        return cert;
    }
 
    public static SSLSocket convertToSecureSocket( Socket baseSocket, String X500name ) throws Exception {
        KeyStore keyStore = KeyStore.getInstance( "JKS" );
 
        if ( !keyStoreFile.exists() ) {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance( "RSA" );
            SecureRandom random = SecureRandom.getInstance( "SHA1PRNG", "SUN" );
            keyGen.initialize( 1024, random );
 
            KeyPair pair = keyGen.generateKeyPair();
            PrivateKey priKey = pair.getPrivate();
            PublicKey pubKey = pair.getPublic();
 
            X509Certificate cert = SecurityTools.generateCertificate( X500name, pair, 365, "SHA256WithRSA" );
 
            keyStore.load( null );
            keyStore.setCertificateEntry( "ServerCert", cert );
            keyStore.setKeyEntry( "ServerPrivateKey", ( Key ) priKey, "".toCharArray(), new Certificate[] {cert} );
            keyStore.store( new FileOutputStream( keyStoreFile ), "".toCharArray() );
        } else {
            keyStore.load(new FileInputStream(keyStoreFile), "".toCharArray());
        }
        TrustManager[] trustAll = new TrustManager[] {
            new X509TrustManager() {
                public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                }
 
                public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                }
 
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            }
        };
 
        KeyManagerFactory kmf = KeyManagerFactory.getInstance( KeyManagerFactory.getDefaultAlgorithm() );
        kmf.init( keyStore, "".toCharArray() );
 
        SSLContext sslContext = SSLContext.getInstance( "SSLv3" );
        sslContext.init( kmf.getKeyManagers(), trustAll, new SecureRandom() );
 
        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
 
        SSLSocket sslSocket = ( SSLSocket ) sslSocketFactory.createSocket(baseSocket, null, baseSocket.getPort(), false);
        sslSocket.setUseClientMode( false );
        sslSocket.setEnabledProtocols(new String[] {"TLSv1", "TLSv1.1", "TLSv1.2", "SSLv3"});
 
        return sslSocket;
    }
 
    public static boolean isSSLPacket( BufferedInputStream input ) throws IOException {
        input.mark( 10 );
        byte[] fingerPrint = new byte[ 10 ];
        input.read( fingerPrint, 0, fingerPrint.length );
        input.reset();
 
        if ( fingerPrint[ 0 ] == 0x16 && fingerPrint[ 1 ] == 0x03 && fingerPrint[ 5 ] == 0x01 ) {
            return true;
        } else {
            return false;
        }
    }
}

