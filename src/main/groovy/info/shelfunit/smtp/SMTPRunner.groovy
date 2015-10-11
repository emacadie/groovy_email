package info.shelfunit.smtp

import groovy.util.logging.Slf4j 

import info.shelfunit.mail.ConfigHolder
import info.shelfunit.mail.meta.MetaProgrammer

@Slf4j
class SMTPRunner {

    static main( args ) {
        MetaProgrammer.runMetaProgramming()
        log.info "in SMTPRunner"
        ConfigHolder.instance.setConfObject( args[ 0 ] )
        def config = ConfigHolder.instance.getConfObject()
        def serverList = [ config.smtp.server.name ]
        config.smtp.other.domains.isEmpty() ?: ( serverList += config.smtp.other.domains )
        SMTPServer smtp = new SMTPServer( serverList )
        log.info "About to call doStuff with port ${config.smtp.server.port.toInteger()} and it's a ${config.smtp.server.port.toInteger().getClass().getName()}"
        smtp.doStuff( config.smtp.server.port.toInteger() )
    }
}

