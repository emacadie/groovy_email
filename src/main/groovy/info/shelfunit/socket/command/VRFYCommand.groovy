package info.shelfunit.socket.command

import groovy.util.logging.Slf4j 

import groovy.sql.Sql

import visibility.Hidden

@Slf4j
class VRFYCommand {
    
    @Hidden Sql sql
    VRFYCommand( def argSql ) {
        this.sql = argSql
    }
    
    def process( theMessage, prevCommandSet, bufferMap ) {
        def resultString
        def resultMap = [:]
        resultMap.clear()
        resultMap.bufferMap = bufferMap
        resultMap.prevCommandSet = prevCommandSet
    }
}

