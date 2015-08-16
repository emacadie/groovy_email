package info.shelfunit.mail

import groovy.lang.Singleton 
import groovy.util.ConfigObject 
import groovy.util.ConfigSlurper

// import visibility.Hidden

@Singleton
class ConfigHolder {
    ConfigObject confObject
    
    def setConfObject( String filePath ) {
        confObject = new ConfigSlurper().parse( new File( filePath ).toURL() )
    }
    
    def returnConfObject( String filePath ) {
        confObject = new ConfigSlurper().parse( new File( filePath ).toURL() )
        confObject
    }
    
    def returnDbMap() {
        def db = [ url: "jdbc:postgresql://${confObject.database.host_and_port}/${confObject.database.dbname}",
        user: confObject.database.dbuser, password: confObject.database.dbpassword, driver: confObject.database.driver ]
    }
    
}

