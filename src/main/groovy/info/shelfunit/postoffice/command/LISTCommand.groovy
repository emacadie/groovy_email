package info.shelfunit.postoffice.command

import groovy.sql.Sql
import groovy.util.logging.Slf4j 

import visibility.Hidden

@Slf4j
class LISTCommand {
    
    @Hidden Sql sql
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

        if ( bufferMap.state != 'TRANSACTION' ) {
            resultMap.resultString = "-ERR Not in TRANSACTION state"
        } else if ( !theMessage.startsWith( 'LIST' ) ) {
            resultMap.resultString = "-ERR Command not in proper form"
        } else if ( theMessage == 'LIST' ) {
            def rows = sql.rows( 'select length( text_body ) from mail_store where username = ? and msg_timestamp < ? order by msg_timestamp', bufferMap.userInfo.username, bufferMap.timestamp )
            def sBuff = new StringBuffer()
            sBuff << "+OK ${bufferMap.totalMessageSize}\r\n"
            rows.eachWithIndex { r, i ->
                sBuff << "${i + 1} ${r.length}"
                if ( i != rows.size() ) { 
                    sBuff << "\r\n" 
                }
            } 
            resultMap.resultString = sBuff.toString()
        } else if ( theMessage.matches( "LIST\\s\\d+" ) ) {
            def messageNum = Integer.parseInt( theMessage.allButFirstFour().trim() )
            
            def ans = sql.firstRow( "select length( text_body ) from mail_store where id = ?", [ bufferMap.uuidList[ messageNum - 1 ] ] )
            if ( ans.isEmpty() ) {
                resultMap.resultString = "-ERR no such message, only ${bufferMap.uuidList.size()} messages in maildrop"
            } else {
                resultMap.resultString = "+OK ${messageNum - 1} ${ans.length()}"
            }
        }
        resultMap.bufferMap = bufferMap
        resultMap.prevCommandSet = prevCommandSet

		log.info "here is resultMap: ${resultMap.toString()}"
		resultMap
    }
}

