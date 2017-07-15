package info.shelfunit.postoffice.command

import groovy.sql.Sql
import groovy.util.logging.Slf4j 

@Slf4j
class LISTCommand {
    
    final Sql sql
    LISTCommand( def argSql ) {
        log.info "Starting new LISTCommand"
        this.sql = argSql
    }
    
    def process( theMessage, prevCommandSet, bufferMap ) {
        log.info "Starting LISTCommand.process"
        
        def resultString
        def resultMap = [:]
        resultMap.clear()
        log.info "Here is bufferMap: ${bufferMap}"
        log.info "Does bufferMap.hasSTATInfo() sez the lolcat ? let's find out: ${bufferMap.hasSTATInfo()}"
        if ( !bufferMap.hasSTATInfo() ) {
            bufferMap.getSTATInfo( sql )
        }
        log.info "Does bufferMap.hasSTATInfo() sez the lolcat ? let's find out: ${bufferMap.hasSTATInfo()}"
        if ( bufferMap.state != 'TRANSACTION' ) {
            resultMap.resultString = "-ERR Not in TRANSACTION state"
        } else if ( !theMessage.startsWith( 'LIST' ) ) {
            resultMap.resultString = "-ERR Command not in proper form"
        } else if ( theMessage == 'LIST' ) {
            def rows = sql.rows( 'select length( text_body ) from mail_store where username = ? and msg_timestamp < ? order by msg_timestamp', bufferMap.userInfo.username, bufferMap.timestamp )
            def sBuff = new StringBuilder()
            sBuff << "+OK ${bufferMap.totalMessageSize}\r\n"
            rows.eachWithIndex { r, i ->
                sBuff << "${i + 1} ${r.length}"
                sBuff << "\r\n" 
            } 
            sBuff << "."
            resultMap.resultString = sBuff.toString()
        } else if ( theMessage.matches( "LIST\\s\\d+" ) ) {
            log.info "in the reg ex part"
            def messageNum = theMessage.getIntInPOP3Command() 
            if ( messageNum > bufferMap.uuidList.size() ) {
                resultMap.resultString = "-ERR no such message, only ${bufferMap.uuidList.size()} messages in maildrop"
            } else {
                def uuid = bufferMap.uuidList[ messageNum - 1 ].id
                log.info "here is bufferMap.uuidList: ${bufferMap.uuidList}"
                log.info "uuid is a ${uuid.getClass().name}"
                def ans = sql.firstRow( "select length( text_body ) from mail_store where id = ?", [ uuid ] )
                if ( ans.isEmpty() ) {
                    resultMap.resultString = "-ERR no such message, only ${bufferMap.uuidList.size()} messages in maildrop"
                } else {
                    resultMap.resultString = "+OK ${messageNum} ${ans.length}"
                }
            }
        }
        resultMap.bufferMap      = bufferMap
        resultMap.prevCommandSet = prevCommandSet
        
        log.info "here is resultMap: ${resultMap.toString()}"
        resultMap
    } // line 65
}

