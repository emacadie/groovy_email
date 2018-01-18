package info.shelfunit.mail

import groovy.util.CliBuilder 
import groovy.util.logging.Slf4j 
import groovy.sql.Sql

import org.apache.commons.cli.ParseException

import org.apache.shiro.crypto.hash.Sha512Hash

import visibility.Hidden

@Slf4j
class UserInserter {
    
    @Hidden def cli 
    @Hidden def options
    
    UserInserter( def args ) {
        this.buildCli()
        try {
            options = cli.parse( args )
            log.info "Options: ${options}"
            if ( options.configPath ) {
                log.info "Here is options.configPath: ${options.configPath}"
                ConfigHolder.instance.setConfObject( options.configPath )
                
                def db = ConfigHolder.instance.returnDbMap() 
                def userMap = [ iterations: options.iterations, user: options.user, userLc: options.user.toLowerCase(), fName: options.fName, lName: options.lName, pass: options.pass ]
                createUser( db, userMap )
            }
            
        } catch ( Exception e ) {
            log.error "Exception: ", e
        }
    }
     /**
     Expects a map with info for database connection, with following keys: dbMap.url, dbMap.user, dbMap.password, dbMap.driver.
     Also a map with information about the user, with the following keys: userMap.iterations, userMap.user, userMap.fName, userMap.lName, userMap.pass
     */
    def createUser( dbMap, userMap ) {
        
        try {
            def sqlObject = Sql.newInstance( dbMap.url, dbMap.user, dbMap.password, dbMap.driver )
                
            log.info "Here is userMap.iterations: ${userMap.iterations}"
            log.info "Here is userMap.user: ${userMap.user}"
            log.info "Here is userMap.fName: ${userMap.fName}"
            log.info "Here is userMap.lName: ${userMap.lName}"
            log.info "Here is userMap.pass: ${userMap.pass}"
            def numIterations = ( userMap.iterations.toInteger() ) * 1000
            def salt = options.user
            def hashedPass = new Sha512Hash( userMap.pass, userMap.user.toLowerCase(), numIterations )
            def base64Hash = "${Character.MIN_VALUE}${userMap.user}${Character.MIN_VALUE}${userMap.pass}".bytes.encodeBase64().toString()
            def params = [ userMap.user, userMap.user.toLowerCase(), hashedPass.toBase64(), 'SHA-512', numIterations, base64Hash, userMap.fName, userMap.lName, 0 ]
            log.info "params: ${params}"
            sqlObject.execute "insert into email_user( username, username_lc, password_hash, " +
                "password_algo, iterations, base_64_hash, first_name, " +
                "last_name, version, logged_in ) values ( ?, ?, ?, ?, ?, ?, ?, ?, ? )", params
           
        } catch ( Exception pe ) {
            pe.printStackTrace()
        }
    }

    def buildCli() {
        try {
            cli = new CliBuilder( usage:'UserInserter' )
            cli.configPath( args: 1, argName:'file', 'path to application.conf file' )
            cli.user( args: 1, argName: 'username', 'user name (part of email address before "@" symbol)' )
            cli.fName( args: 1, argName: 'first name', "user's first name" )
            cli.lName( args: 1, argName: 'last name', "user's last name" )
            cli.pass( args: 1, argName: 'password', "password" )
            cli.iterations( args: 1, argName: 'kilo-iterations', "number of thousands of iterations to hash password" )
            cli.usage()
        } catch ( ParseException pe ) {
            pe.printStackTrace()
        }
    }
    
    def getConfigFromCli() {
    }
    
    def addUser() {
    }
    
    static main( args ) {
        log.info "in UserInserters"
        
        def uInsert = new UserInserter( args )
        
    }
}

