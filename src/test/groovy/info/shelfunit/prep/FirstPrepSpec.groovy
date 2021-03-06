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
    // you could also make a private static method to check the prop:
    // http://mrhaki.blogspot.com/2014/06/spocklight-ignore-specifications-based.html
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
                iterations bigint not null,
                first_name character varying(30) not null,
                last_name character varying(30) not null,
                version bigint NOT NULL
            )'''
            
         sql.execute '''
             create table mail_store (
                id UUID PRIMARY KEY  NOT NULL unique,
                username character varying( 64 ) not null,
                from_address character varying( 255 ) not null,
                to_address character varying( 255 ) not null,
                message bytea,
                text_body text not null,
                msg_timestamp TIMESTAMP WITH TIME ZONE default clock_timestamp() not null,
                FOREIGN KEY ( username ) REFERENCES email_user ( username ) on delete cascade
            )
         '''
         expect:
            5 == 5
    }
}


