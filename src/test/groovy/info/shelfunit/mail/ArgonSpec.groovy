package info.shelfunit.mail

import groovy.util.logging.Slf4j

import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Stepwise
// import spock.lang.Unroll

import org.junit.Rule
import org.junit.rules.TestName

// import de.mkammerer.argon2.Argon2
// import de.mkammerer.argon2.Argon2Factory

// import com.lambdaworks.crypto.SCryptUtil

// import org.mindrot.jbcrypt.BCrypt

// import info.shelfunit.mail.ConfigHolder
import info.shelfunit.mail.meta.MetaProgrammer



@Slf4j
@Stepwise
class ArgonSpec extends Specification {
    
  
    @Rule 
    TestName name = new TestName()

    def setup() {
        println "\n--- Starting test ${name.methodName}"
    }   // run before every feature method
    def cleanup() {} // run after every feature method
    
    def setupSpec() {
        MetaProgrammer.runMetaProgramming()
        ConfigHolder.instance.setConfObject( "src/test/resources/application.test.conf" )
        // sql = ConfigHolder.instance.getSqlObject() 
        // this.addUsers()
        // deleCommand = new DELECommand( sql )
    } // run before the first feature method
    
    def cleanupSpec() {
        // sql.execute "DELETE FROM email_user where username in ( ${gwDELE}, ${jaDELE}, ${joDELE} )"
        // sql.close()
    } // run after the last feature method

    @Ignore
    def "ignore this"() {}
/*
    def "try Some Argon"() {
        when:
            // Create instance
            Argon2 argon2 = Argon2Factory.create();

            // Read password from user
            char [] password = "correct horse battery staple" as char []

            try {
                // Hash password
                long startA = System.nanoTime()
                String hashA = argon2.hash( 
                    2,       // int iterations
                    65536,   // int memory
                    1,       // int parallelism
                    password // String password
                );
                long endA = System.nanoTime()
                println "hashA done in " + ( ( endA - startA )/1.0e9 )

                long startB = System.nanoTime()
                String hashB = argon2.hash( 
                    30,       // int iterations
                    65536,    // int memory
                    1,        // int parallelism
                    password  // String password
                );
                long endB = System.nanoTime()
                println "hashB done in " + ( ( endB - startB )/1.0e9 )

                // Verify password
                if ( argon2.verify( hashA, password ) ) {
                    println "hashA matches password"
                } else {
                    println "hash doesn't match password"
                }
                if ( argon2.verify( hashB, password ) ) {
                    println "hashB matches password"
                } else {
                    println "hashB doesn't match password"
                }
                println "Here is password: " + password
                println "Here is hashA: " + hashA
                println "Size of hashA: " + hashA.size() + "; length of hashA: " + hashA.length()
                println "Here is hashB: " + hashB
                println "Size of hashB: " + hashB.size() + "; length of hashB: " + hashB.length()
                println "65536 / 1024 is: " + (65536 / 1024)
            } finally {
                // Wipe confidential data
                argon2.wipeArray( password )
            }
        then:
            1 == 1
    }
*/
/*
    def "try some scrypt"() {
        when:
            def password = "correct horse battery staple"
            // # Calculating a hash
            int cpuCost  = 16384;
            int memCost  = 8;
            int parlParm = 1;
            String hashed = SCryptUtil.scrypt( 
                password, // passwd - Password.
                cpuCost,  // N - CPU cost parameter.
                memCost,  // r - Memory cost parameter.
                parlParm  // p - Parallelization parameter.
            );

            String hashed2 = SCryptUtil.scrypt( 
                password, // passwd - Password.
                cpuCost,  // N - CPU cost parameter.
                memCost,  // r - Memory cost parameter.
                parlParm  // p - Parallelization parameter.
            );

            //# Validating a hash
            if ( SCryptUtil.check( password, hashed ) ) {
                println "Login successful"
            }

            println "Here is 16384 / 1024: " + ( 16384 / 1024 )
            println "Here is password: " + password
            println "Here is hashed: "   + hashed 
            println "hashed length of "  + hashed.length()
            println "Here is hashed2: "  + hashed2 
            println "hashed2 length of " + hashed2.length()
        then:
            1 == 1
    }

*/
/*
    def "try some jbcrypt"() {
        when:
            
            // Hash a password for the first time
            String password = "correct horse battery staple"
            String hashedA1 = BCrypt.hashpw( password, BCrypt.gensalt() )
            String hashedA2 = BCrypt.hashpw( password, BCrypt.gensalt() )

            // gensalt's log_rounds parameter determines the complexity
            // the work factor is 2**log_rounds, and the default is 10
            String hashedB1 = BCrypt.hashpw( password, BCrypt.gensalt( 12 ) )
            String hashedB2 = BCrypt.hashpw( password, BCrypt.gensalt( 12 ) )

            // Check that an unencrypted password matches one that has
            // previously been hashed
            [ "a1": hashedA1, "a2": hashedA2, "b1": hashedB1, "b2": hashedB2 ].each { name, hPass ->
                if ( BCrypt.checkpw( password, hashedA1 ) ) {
                    System.out.println( "It matches " + name );
                } else {
                    System.out.println( "It does not match " + name );
                }
            }

            println "Here is password: "  + password
            println "Here is hashedA1: "  + hashedA1 
            println "hashedA1 length of " + hashedA1.length()
            println "Here is hashedA2: "  + hashedA2 
            println "hashedA2 length of " + hashedA2.length()
            println "Here is hashedB1: "  + hashedB1 
            println "hashedB1 length of " + hashedB1.length()
            println "Here is hashedB2: "  + hashedB2 
            println "hashedB2 length of " + hashedB2.length()
        then:
            1 == 1
    } // try some jbcrypt
*/
} // end class ArgonSpec
