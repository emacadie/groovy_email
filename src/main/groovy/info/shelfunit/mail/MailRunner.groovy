package info.shelfunit.mail

// import groovy.util.ConfigSlurper
import groovy.util.logging.Slf4j 
// import groovy.sql.Sql

// import info.shelfunit.mail.ConfigHolder

@Slf4j
class MailRunner {

    static main( args ) {
        MetaProgrammer.runMetaProgramming()
        log.info "in MailRunner"
        ConfigHolder.instance.setConfObject( args[ 0 ] )
        def config = ConfigHolder.instance.getConfObject()
        def serverList = [ config.smtp.server.name ]
        config.smtp.other.domains.isEmpty() ?: ( serverList += config.smtp.other.domains )
        SMTPServer smtp = new SMTPServer( serverList )
        smtp.doStuff( config.smtp.server.port.toInteger() )
    }
}

