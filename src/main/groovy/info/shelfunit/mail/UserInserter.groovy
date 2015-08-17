package info.shelfunit.mail

// import groovy.util.ConfigSlurper
import groovy.util.CliBuilder 
import groovy.util.logging.Slf4j 
import groovy.sql.Sql

import org.apache.shiro.crypto.hash.Sha512Hash
// import info.shelfunit.mail.ConfigHolder

@Slf4j
class UserInserter {
    
    UserInserter( def args ) {
        
        def cli = new CliBuilder( usage:'UserInserter' )
        cli.configPath( 'path to application.conf file' )
        cli.user( 'user name (part of email address before "@" symbol)' )
        cli.fName( "user's first name" )
        cli.lName( "user's last name" )
        cli.pass( "password" )
        cli.iterations( "number of thousands of iterations to hash password" )

        def options = cli.parse( args )
        
        ConfigHolder.instance.setConfObject( options.configPath )
        // def config = ConfigHolder.instance.getConfObject()
        def db = ConfigHolder.instance.returnDbMap()         
        
        sql = Sql.newInstance( db.url, db.user, db.password, db.driver )
        
        def numIterations = options.iterations
        def salt = options.user
        def atx512 = new Sha512Hash( options.pass, options.user, numIterations )
        def params = [ options.user, atx512.toBase64(), 'SHA-512', numIterations, options.fName, options.lName, 0 ]
        sql.execute 'insert into  email_user( username, password_hash, password_algo, iterations, first_name, last_name, version ) values ( ?, ?, ?, ?, ?, ?, ? )', params
    }

    def getConfigFromCli() {
    }
    
    def addUser() {
    }
    
    static main( args ) {
        // MetaProgrammer.runMetaProgramming()
        log.info "in UserInserters"
        
        def uInsert = new UserInserter( args )
    }
}

