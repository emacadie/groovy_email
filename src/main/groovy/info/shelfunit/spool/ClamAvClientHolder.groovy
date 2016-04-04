package info.shelfunit.spool

import groovy.util.logging.Slf4j 
import java.io.IOException

import info.shelfunit.mail.ConfigHolder
// import java.io.IOException
import fi.solita.clamav.ClamAVClient

@Slf4j 
class ClamAvClientHolder {
    
    static getClamAvClient() {
        def config = ConfigHolder.instance.getConfObject()
        def host   = config.clamav.hostname
        def port   = config.clamav.port
        log.info "About to return new client"
        return new ClamAVClient( host, port.toInt() )
    }
    
    static checkClam( clamavj ) {
        def result
        try {
            result = clamavj.ping()
        } catch ( IOException ioEx ) {
            result = false
        }
        log.info "result of checkClam() is ${result}"
        result
    }
}

