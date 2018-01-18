package info.shelfunit.spool

import fi.solita.clamav.ClamAVClient
import groovy.sql.Sql
import info.shelfunit.mail.ConfigHolder

class SpoolRunner {
    
    private config
    final Sql sqlObject
    def iSW
    def clamavj
    
    SpoolRunner() {
        this.config = ConfigHolder.instance.getConfObject()
        sqlObject   = ConfigHolder.instance.getSqlObject()
        iSW         = new InboundSpoolWorker()
        clamavj     = createClamAVClient( config.clamav.hostname, config.clamav.port )
    }
    
    def createClamAVClient( host, port ) {
        return new ClamAVClient( host, port.toInt() )
    }
    
    def checkClam() {
        def result
        try {
            result = clamavj.ping()
        } catch ( IOException ioEx ) {
            result = false
        }
        log.info "result of checkClam() is ${result}"
        result
    }
    
    def runClamInbound() {
        if ( checkClam() ) {
            iSW.runClam( sqlObject, clamavj )
        }
    }
}

