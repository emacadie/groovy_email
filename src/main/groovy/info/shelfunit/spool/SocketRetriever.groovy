package info.shelfunit.spool

import org.xbill.DNS.Lookup
import org.xbill.DNS.MXRecord
import org.xbill.DNS.Record
import org.xbill.DNS.Type

import groovy.util.logging.Slf4j 

@Slf4j 
class SocketRetriever {
    
    def host
    def port
    
    SocketRetriever( argHost, argPort ) {
        this.host = argHost
        this.port = argPort
    }
    
    def getSocket() {
        def records = new Lookup( host, Type.MX ).run()
        def targetMap = [:]
        def reverseMap = [:]
        def priorityList = []
        records.each { record ->
            MXRecord mx = ( MXRecord ) record
            log.info( "Host " + mx.target + " has preference " + mx.priority + " which is a " + mx.priority.getClass().name + " with TTL " + mx.ttl )
            targetMap[ mx.target ] = mx.priority
            priorityList << mx.priority
            reverseMap[ mx.priority ] = mx.target
        }
        log.info ""
        targetMap.each { t, p ->
            log.info "target ${t} has priority ${p}"
        }
        log.info "reverse map"
        reverseMap.each { k, v ->
            log.info "target ${v} has priority ${k}"
        }
        log.info "Minimum priority is ${priorityList.min()}"
        log.info "So use server ${reverseMap[ priorityList.min() ]}"
        def socket = new Socket( reverseMap[ priorityList.min() ], port )
        return socket
    }
}

