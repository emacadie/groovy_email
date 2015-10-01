package info.shelfunit.postoffice

import groovy.util.logging.Slf4j 

import info.shelfunit.mail.ConfigHolder
import info.shelfunit.mail.MetaProgrammer

@Slf4j
class PostOfficeRunner {

    static main( args ) {
        MetaProgrammer.runMetaProgramming()
        log.info "in PostOfficeRunner"
        ConfigHolder.instance.setConfObject( args[ 0 ] )
        def config = ConfigHolder.instance.getConfObject()
        def serverList = [ config.smtp.server.name ]
        config.smtp.other.domains.isEmpty() ?: ( serverList += config.smtp.other.domains )
        PostOfficeServer postO = new PostOfficeServer( serverList )
        postO.doStuff( config.postoffice.server.port.toInteger() )
    }
}

