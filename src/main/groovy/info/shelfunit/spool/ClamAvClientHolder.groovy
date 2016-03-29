package info.shelfunit.spool

import groovy.util.logging.Slf4j 
import info.shelfunit.mail.ConfigHolder
// import java.io.IOException
import fi.solita.clamav.ClamAVClient

@Slf4j 
class ClamAvClientHolder {
    
    static getClamAvClient() {
        def host = ConfigHolder.instance.clamav.hostname
        def port = ConfigHolder.instance.clamav.port
        log.info "About to return new client"
        return new ClamAVClient( host, port.toInt() )
    }
}

