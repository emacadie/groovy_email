package info.shelfunit.mail

// import info.shelfunit.mail.ConfigHolder

import groovy.sql.Sql
import org.apache.shiro.crypto.hash.Sha512Hash

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
    
    static iterations = 10000
    
    static addUser( sql, firstName, lastName, userName, password, loggedIn = false ) {
        def params = [ userName, ( new Sha512Hash( password, userName, iterations ).toBase64() ), 'SHA-512', iterations, getBase64Hash( userName, password ), firstName, lastName, 0, loggedIn ]
        sql.execute 'insert into  email_user( username, password_hash, password_algo, iterations, base_64_hash, first_name, last_name, version, logged_in ) values ( ?, ?, ?, ?, ?, ?, ?, ?, ? )', params
    }
    
    static addMessage( def sql, def uuid, def userName, def messageString, def domain, def fromAddress = "hello@test.com" ) {
        def toAddress = "${userName}@${domain}".toString()
        def params = [ uuid, userName, fromAddress, toAddress, messageString ]
        sql.execute 'insert into mail_store(id, username, from_address, to_address, text_body) values (?, ?, ?, ?, ?)', params
    }
    
    
    static getUserId( sql, userName ) {
        def userResult = sql.firstRow( 'select userid from email_user where username = ?', [ userName ] )
        return userResult.userid
    }
}

