package info.shelfunit.postoffice.command

import groovy.sql.Sql
import groovy.util.logging.Slf4j 
import java.sql.SQLException

@Slf4j
class QUITCommand {
    
    final def regex = "RETR\\s\\d+"
    
    final Sql sql
    final serverName
    
    QUITCommand( def argSql, def argServerName ) {
        log.info "Starting new QUITCommand"
        this.sql = argSql
        this.serverName = argServerName
    }
    
    def process( theMessage, prevCommandSet, bufferMap ) {
        log.info "Starting QUITCommand.process"
   
        def resultString
        def resultMap = [:]
        resultMap.clear()
        log.info "Here is bufferMap: ${bufferMap}"
        log.info "Does bufferMap.hasSTATInfo() sez the lolcat ? let's find out: ${bufferMap.hasSTATInfo()}"
        
        if ( theMessage != 'QUIT' ) {
            resultMap.resultString = "-ERR Command not in proper form"
        } else if ( bufferMap.state == 'AUTHORIZATION' ) {
            bufferMap.clear()
            resultMap.resultString = "+OK ${serverName}  POP3 server signing off"
        } else if ( bufferMap.state == 'TRANSACTION' ) {
            if ( !bufferMap.hasSTATInfo() && !bufferMap.userInfo ) {
                bufferMap.getSTATInfo( sql )
            }
            log.info "in the reg ex part"
            log.info "here is bufferMap in QUITCommand.process: ${bufferMap}"
            def idsToDelete = bufferMap.deleteMap?.values() as List
            resultMap.resultString = "+OK ${serverName} POP3 server signing off"
            
            def result = '250 OK'
            log.info "here is bufferMap.userInfo.userid: ${bufferMap.userInfo.userid} and it's a ${bufferMap.userInfo.userid.getClass().name}"
            if ( ( this.changeLoggedInFlag( bufferMap.userInfo.userid ) == '250 OK' ) && 
            ( this.deleteMessages( idsToDelete ) == '250 OK' ) ) {
                bufferMap.clear()
                bufferMap.state = 'UPDATE'
            } else {
                result = '500 Something went wrong'
            }
        }
        resultMap.bufferMap = bufferMap
        resultMap.prevCommandSet = prevCommandSet

		log.info "here is resultMap: ${resultMap.toString()}"
		resultMap
    }
    
    private changeLoggedInFlag( def argUserid ) {
        def result = '250 OK'
        log.info "in changedLoggedInFlag with arg ${argUserid}"
        try {
            sql.executeUpdate "UPDATE email_user set logged_in = ? where userid = ?", [ false, argUserid ]
        } catch ( SQLException ex ) {
            result = '500 Something went wrong'
        }
        return result
    }
    
    private deleteMessages( def idsToDelete ) {
        def result = '250 OK'
        if ( idsToDelete ) {
            try {
                log.info "here is idsToDelete: ${idsToDelete} and it is a ${idsToDelete.getClass().name}"
                sql.execute "DELETE from mail_store where id in (${ idsToDelete.getQMarkString() })", idsToDelete
                log.info "Called the delete command"
            } catch ( Exception e ) {
                result = '500 Something went wrong'
                log.error "Here is exception: ", e
                SQLException ex = e.getNextException()
                log.info "Next exception message: ${ex.getMessage()}"
                log.error "something went wrong", ex 
            }
        }
        return result
    }
}

