package info.shelfunit.smtp

import groovy.util.logging.Slf4j 

import info.shelfunit.mail.ConfigHolder
import info.shelfunit.mail.MetaProgrammer
// import info.shelfunit.smtp.SMTPServer

@Slf4j
class SMTPRunner {

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

