package info.shelfunit.mail

// import groovy.util.ConfigSlurper
import groovy.util.CliBuilder 
import groovy.util.logging.Slf4j 
import groovy.sql.Sql

import org.apache.commons.cli.ParseException

import org.apache.shiro.crypto.hash.Sha512Hash
// import info.shelfunit.mail.ConfigHolder

@Slf4j
class UserInserter {
    
    UserInserter( def args ) {
        try {
        
            def cli = new CliBuilder( usage:'UserInserter' )
            cli.configPath( args: 1, argName:'file', 'path to application.conf file' )
            cli.user( args: 1, argName: 'username', 'user name (part of email address before "@" symbol)' )
            cli.fName( args: 1, argName: 'first name', "user's first name" )
            cli.lName( args: 1, argName: 'last name', "user's last name" )
            cli.pass( args: 1, argName: 'password', "password" )
            cli.iterations( args: 1, argName: 'kilo-iterations', "number of thousands of iterations to hash password" )
            cli.usage()
    
            def options = cli.parse( args )
            log.info "Options: ${options}"
            if ( options.configPath ) {
                log.info "Here is options.configPath: ${options.configPath}"
                ConfigHolder.instance.setConfObject( options.configPath )
                
                // def config = ConfigHolder.instance.getConfObject()
                def db = ConfigHolder.instance.returnDbMap()         
                
                def sql = Sql.newInstance( db.url, db.user, db.password, db.driver )
                
                log.info "Here is options.iterations: ${options.iterations}"
                log.info "Here is options.user: ${options.user}"
                log.info "Here is options.fName: ${options.fName}"
                log.info "Here is options.lName: ${options.lName}"
                log.info "Here is options.pass: ${options.pass}"
                def numIterations = Integer.parseInt( options.iterations ) * 1000
                def salt = options.user
                def atx512 = new Sha512Hash( options.pass, options.user, numIterations )
                def params = [ options.user, atx512.toBase64(), 'SHA-512', numIterations, options.fName, options.lName, 0 ]
                log.info "params: ${params}"
                sql.execute 'insert into  email_user( username, password_hash, password_algo, iterations, first_name, last_name, version ) values ( ?, ?, ?, ?, ?, ?, ? )', params
            }
        } catch ( ParseException pe ) {
            pe.printStackTrace()
        }
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

