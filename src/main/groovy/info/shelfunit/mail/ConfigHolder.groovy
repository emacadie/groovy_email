package info.shelfunit.mail

import groovy.lang.Singleton 
import groovy.sql.Sql
import groovy.util.ConfigObject 
import groovy.util.ConfigSlurper

@Singleton
class ConfigHolder {
    ConfigObject confObject
    
    Sql sqlObject = null
    
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
        return db
    }
    
    def getSqlObject() {
        return sqlObject ?: Sql.newInstance( 
            "jdbc:postgresql://${confObject.database.host_and_port}/${confObject.database.dbname}", // db.url,
            confObject.database.dbuser,      // db.user, 
            confObject.database.dbpassword, // db.password, 
            confObject.database.driver      // db.driver 
        )
        
    }
    
}

