package info.shelfunit.teardown

import spock.lang.Specification
import spock.lang.Stepwise

import spock.lang.Shared

import spock.lang.Requires

import org.junit.Rule
import org.junit.rules.TestName

import groovy.sql.Sql

@Stepwise
class FirstTeardownSpec extends Specification {
    
    @Shared
    static teardownDatabase = false
    
    static sql
    
    @Rule 
    TestName name = new TestName()
    
    def setup() {
        println "\n--- Starting test ${name.methodName}"
    }          // run before every feature method
    def cleanup() {}        // run after every feature method
    
    def setupSpec() {
        println "In ${this.class.name}"
        println "System.properties[ 'database.teardown' ]: ${System.properties[ 'database.teardown' ]}"
        println "System.properties['database.teardown']: ${System.properties['database.teardown']}"
        if ( System.getProperty( "database.teardown" ) == 'true' ) {
            println "database.teardown is true"
            teardownDatabase = true
            def db = [url: "jdbc:postgresql://${System.properties[ 'host_and_port' ]}/${System.properties[ 'dbname' ]}",
            user: System.properties[ 'dbuser' ], password: System.properties[ 'dbpassword' ], driver: 'org.postgresql.Driver']
            sql = Sql.newInstance(db.url, db.user, db.password, db.driver)
            
        } else {
            println "database.teardown not set"
            teardownDatabase = false
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
    @Requires({ properties[ 'database.teardown' ] == 'true' })
    def "run if database teardown is true"() {
        println "teardownDatabase: ${teardownDatabase}"
        expect:
            4 == 4
    }
    
    @Requires({ properties[ 'database.teardown' ] == 'true' })
    def "drop user table"() {
        
        sql.execute "DROP TABLE IF EXISTS email_user CASCADE"
        sql.execute "DROP TABLE IF EXISTS mail_store CASCADE"
         expect:
            5 == 5
    }
}


