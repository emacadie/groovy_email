package info.shelfunit.mail

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
    static sqlObject = null
    static getSqlObject() {
        GETestUtils.getDb()
       
        sqlObject ?: Sql.newInstance( db.url, db.user, db.password, db.driver )
        return new Sql( sqlObject )
    }
    
    static def getBase64Hash( username, password ) {
        "${Character.MIN_VALUE}${username.toLowerCase()}${Character.MIN_VALUE}${password}".bytes.encodeBase64().toString()
    }
    
    static def alphabet =  ( 'a'..'z' ).join() + ( 'A'..'Z' ).join()
    
    static def getRandomString = { int n = 12 ->
        new Random().with {
            ( 1..n ).collect { 
                alphabet[ nextInt( alphabet.length() ) ] 
            }.join()
        }
    }
    
    static iterations = 10000
    
    static addUser( sqlObject, firstName, lastName, userName, password, loggedIn = false ) {
        def params = [ userName, userName.toLowerCase(), 
                       ( new Sha512Hash( password, userName.toLowerCase(), iterations ).toBase64() ), 
                       'SHA-512', iterations, getBase64Hash( userName.toLowerCase(), password ), firstName, lastName, 0, loggedIn 
        ]
        sqlObject.execute "insert into  email_user( username, username_lc, password_hash, " +
            "password_algo, iterations, base_64_hash, first_name, last_name, version, logged_in )" +
            "values ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )", params
    }
    
    static addMessage( def sqlObject, def uuid, def userName, def messageString, def domain, def fromAddress = "hello@test.com" ) {
        def toAddress = "${userName}@${domain}".toString()
        def params = [ uuid, userName, userName.toLowerCase(), fromAddress, toAddress, messageString ]
        sqlObject.execute "insert into mail_store(id, username, username_lc, " +
            "from_address, to_address, text_body) " +
            "values ( ?, ?, ?, ?, ?, ? )", params
    }
    
    static getUserId( sqlObject, userName ) {
        def userResult = sqlObject.firstRow( 'select userid from email_user where username_lc = ?', [ userName.toLowerCase() ] )
        return userResult.userid
    }
    
    static getUserInfo( sqlObject, userName ) {
        return sqlObject.firstRow( 'select * from email_user where username_lc = ?', userName.toLowerCase() )
    }
    
    static getTableCount( sqlObject, statement, params ) {
        def result = sqlObject.firstRow( statement, params )
        return result.count
    }

    

}

