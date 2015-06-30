package info.shelfunit.prep

import spock.lang.Specification
import spock.lang.Stepwise

import spock.lang.Shared

import spock.lang.Requires

import org.junit.Rule
import org.junit.rules.TestName

@Stepwise
class FirstPrepSpec extends Specification {
    
    @Shared
    static prepDatabase = false
    
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
    
    @Requires({ System.properties[ 'database.prep' ] == 'true' })
    def "run if database prep is true"() {
        println "prepDatabase: ${prepDatabase}"
        expect:
            4 == 4
    }
}


