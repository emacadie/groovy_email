package info.shelfunit.mail

import groovy.util.ConfigSlurper
import groovy.util.logging.Slf4j 
import groovy.sql.Sql

@Slf4j
class MailRunner {

    static main( args ) {
        MetaProgrammer.runMetaProgramming()
        log.info "in MailRunner"
        URL theURL = getClass().getResource( "/log4j.properties" );
        log.info "theURL is a ${theURL.class.name}"
        def db = [ url: "jdbc:postgresql://${System.properties[ 'host_and_port' ]}/${System.properties[ 'dbname' ]}",
        user: System.properties[ 'dbuser' ], password: System.properties[ 'dbpassword' ], driver: 'org.postgresql.Driver' ]
        def sql = Sql.newInstance( db.url, db.user, db.password, db.driver )
        def config = new ConfigSlurper().parse( new File( args[ 0 ] ).toURL() )
        def serverName = config.smtp.server.name
        log.info "Here is config.smtp.server.name: ${config.smtp.server.name}"
        def serverList = [ config.smtp.server.name ]
        config.smtp.other.domains.isEmpty() ?: ( serverList += config.smtp.other.domains )
        SMTPServer smtp = new SMTPServer( serverList )
        smtp.doStuff( config.smtp.server.port.toInteger(), sql )
    }
}

