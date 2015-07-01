package info.shelfunit.prep

import spock.lang.Specification
import spock.lang.Stepwise

import spock.lang.Shared

import spock.lang.Requires

import org.junit.Rule
import org.junit.rules.TestName

import groovy.sql.Sql

@Stepwise
class FirstPrepSpec extends Specification {
    
    @Shared
    static prepDatabase = false
    
    static sql
    
    @Rule 
    TestName name = new TestName()
    
    def setup() {
        println "\n--- Starting test ${name.methodName}"
    }          // run before every feature method
    def cleanup() {}        // run after every feature method
    
    def setupSpec() {
        println "In ${this.class.name}"
        println "System.properties[ 'database.prep' ]: ${System.properties[ 'database.prep' ]}"
        println "System.properties['database.prep']: ${System.properties['database.prep']}"
        if ( System.getProperty( "database.prep" ) == 'true' ) {
            println "database.prep is true"
            prepDatabase = true
            /*
            systemProperties[ 'dbname' ]        = 'gemail_test_db'
            systemProperties[ 'host_and_port' ] = 'localhost:5432'
            systemProperties[ 'dbuser' ]        = 'gemail_test'
            systemProperties[ 'dbpassword' ]    = 'dev-word-to-pass001'
            */
            def db = [url: "jdbc:postgresql://${System.properties[ 'host_and_port' ]}/${System.properties[ 'dbname' ]}",
            user: System.properties[ 'dbuser' ], password: System.properties[ 'dbpassword' ], driver: 'org.postgresql.Driver']
            sql = Sql.newInstance(db.url, db.user, db.password, db.driver)
            
        } else {
            println "database.prep not set"
            prepDatabase = false
        }
    }     // run before the first feature method
    
    def cleanupSpec() {}   // run after the last feature method

    def "first test"() {
        expect:
            1 == 1
    }
    
    def "this is third test and we shall call it...third test"() {
        println "But thanks to @Stepwise it will run second"
        expect:
            3 == 3
    }
    
    def "second test"() {
        expect:
            2 == 2
    }
    
    // in the closure for Requires, you can use "properties" instead of "System.properties"
    @Requires({ properties[ 'database.prep' ] == 'true' })
    def "run if database prep is true"() {
        println "prepDatabase: ${prepDatabase}"
        expect:
            4 == 4
    }
    
    @Requires({ properties[ 'database.prep' ] == 'true' })
    def "create user table"() {
        
        sql.execute '''
            CREATE TABLE email_user (
                userid serial primary key NOT NULL,
                username character varying(64) not null unique,
                password_hash character varying(150) not null,
                password_algo character varying(32) not null,
                first_name character varying(30) not null,
                last_name character varying(30) not null,
                version bigint NOT NULL
            )'''
         expect:
            5 == 5
    }
}


