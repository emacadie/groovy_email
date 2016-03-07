package info.shelfunit.mail

// import info.shelfunit.mail.ConfigHolder

import groovy.sql.Sql

class GETestUtils {
    
    
    static {
        ConfigHolder.instance.setConfObject( "src/test/resources/application.test.conf" )
    }
    static conf = ConfigHolder.instance.getConfObject()
    
    static db = null
    static def getDB() {
        db ?: [ url: "jdbc:postgresql://${conf.database.host_and_port}/${conf.database.dbname}",
        user: conf.database.dbuser, password: conf.database.dbpassword, driver: conf.database.driver ]
        return db
    }
    static sql = null
    static getSql() {
        GETestUtils.getDb()
       
        sql ?: Sql.newInstance( db.url, db.user, db.password, db.driver )
        return new Sql( sql )
    }
    
    static def getBase64Hash( fname, password ) {
        "${Character.MIN_VALUE}${fname}${Character.MIN_VALUE}${password}".bytes.encodeBase64().toString()
    }
    
    static def alphabet =  ( 'a'..'z' ).join()
    
    static def getRandomString = { int n = 9 ->
        
        new Random().with {
            ( 1..n ).collect { 
                alphabet[ nextInt( alphabet.length() ) ] 
            }.join()
        }
    }
}

