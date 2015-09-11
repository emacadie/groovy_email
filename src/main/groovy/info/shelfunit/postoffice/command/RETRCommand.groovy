package info.shelfunit.postoffice.command

import groovy.sql.Sql
import groovy.util.logging.Slf4j 

import visibility.Hidden

@Slf4j
class RETRCommand {
    
    @Hidden def regex = "RETR\\s\\d+"
    
    @Hidden Sql sql
    RETRCommand( def argSql ) {
        log.info "Starting new RETRCommand"
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
        } else if ( !theMessage.matches( regex ) ) {
            resultMap.resultString = "-ERR Command not in proper form"
        } else if ( theMessage.matches( regex ) ) {
            log.info "in the reg ex part"
            def messageNum = Integer.parseInt( theMessage.allButFirstFour().trim() )
            if ( messageNum > bufferMap.uuidList.size() ) {
                resultMap.resultString = "-ERR no such message, only ${bufferMap.uuidList.size()} messages in maildrop"
            } else {
                def uuid = bufferMap.uuidList[ messageNum - 1 ].id
                log.info "here is bufferMap.uuidList: ${bufferMap.uuidList}"
                log.info "uuid is a ${uuid.getClass().name}"
                def ans = sql.firstRow( "select length( text_body ),text_body from mail_store where id = ?", [ uuid ] )
                if ( ans.isEmpty() ) {
                    resultMap.resultString = "-ERR no such message, only ${bufferMap.uuidList.size()} messages in maildrop"
                } else {
                    def sBuff = new StringBuffer()
                    sBuff << "+OK ${ans.length} octets\r\n"
                    sBuff << ans.text_body
                    sBuff << "\r\n"
                    sBuff << "."
                    resultMap.resultString = sBuff.toString() 
                    // def result = sql.execute( "delete from mail_store where id = ?", [ uuid ] )
                }
            }
        }
        resultMap.bufferMap = bufferMap
        resultMap.prevCommandSet = prevCommandSet

		log.info "here is resultMap: ${resultMap.toString()}"
		resultMap
    }
}

