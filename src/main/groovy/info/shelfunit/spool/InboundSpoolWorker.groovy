package info.shelfunit.spool

import groovy.util.logging.Slf4j 
// import info.shelfunit.mail.ConfigHolder
// import java.io.IOException
import java.sql.SQLException
import fi.solita.clamav.ClamAVClient

@Slf4j
class InboundSpoolWorker{
    
    // states of message: ENTERED, CLEAN (clean CalmAV scan) or UNCLEAN (unclean ClamAV scan)
    // TRANSFERRED: a clean message has been copied to mail_store for each user listed in the message
    
    // final config
    final sql
    ClamAVClient clamavj
    static final QUERY_STATUS_STRING = 'select * from mail_spool_in where status_string = ?'
    static final INSERT_STRING = 'insert into mail_store( id, username, from_address, to_address, text_body, msg_timestamp ) values ( ?, ?, ?, ?, ?, ? )'
    static final SELECT_USER_STRING = 'select username from email_user where lower( username ) = ?'
    
    InboundSpoolWorker( ) {
    }
    
    def runClam( sql, clamavj ) {
        def cleanUUIDs = []
        def uncleanUUIDs = []
        sql.eachRow( QUERY_STATUS_STRING, [ 'ENTERED' ] ) { row ->
            byte[] data = row[ 'text_body' ].getBytes()
            InputStream input = new ByteArrayInputStream( data )
            log.info "input is a ${input.getClass().name}"
            def isClean = this.runClamOnMessage( input, clamavj )
            if ( isClean ) {
                cleanUUIDs << row[ 'id' ]
            } else {
                uncleanUUIDs << row[ 'id' ]
            }
        } // sql.eachRow
       this.updateMessageStatus( sql, cleanUUIDs, 'CLEAN' )
       if ( isNot( uncleanUUIDs.isEmpty() ) ) { 
           this.updateMessageStatus( sql, uncleanUUIDs, 'UNCLEAN' ) 
       }
    } // runClam
    
    def updateMessageStatus( sql, uuidList, status ) {
        try {
            log.info "here is idsToDelete: ${uuidList} and it is a ${uuidList.getClass().name}"
            def insertCounts 
            def params = []
            def newObject = uuidList.plus( 0, status )
            log.info "newObject is a ${newObject.getClass().name}, here it is: ${newObject}"
            if ( isNot( uuidList.isEmpty() ) ) { 
                sql.withTransaction {
                    params << status
                    params += uuidList // you can do this, or UUIDs.plus( 0, status ) which adds status to front of list
                    sql.execute( "UPDATE mail_spool_in set status_string = ? where id in (${uuidList.getQMarkString()}) ", uuidList.plus( 0, status ) )
                }
            }
            
        } catch ( Exception e ) {
            log.error "Here is exception: ", e
            SQLException ex = e.getNextException()
            log.info "Next exception message: ${ex.getMessage()}"
            log.error "something went wrong", ex 
        }
    }
    
    def runClamOnMessage( inString, clamavj ) {
        def reply
        def messageIsClean = true
        try {
            reply = clamavj.scan( inString )
            log.info "here is reply: ${reply.toString()} and it's a ${reply.getClass().name}"
        } catch ( Exception e ) {
            messageIsClean = false
            throw new RuntimeException( "Could not scan the input", e )
        }
        log.info "ClamAVClient.isCleanReply( reply ) : ${ClamAVClient.isCleanReply( reply ) }"
        if ( isNot( ClamAVClient.isCleanReply( reply ) ) ) {
            log.info "aaargh. Something was found"
            messageIsClean = false
        }
        return messageIsClean
    }
    
    def moveCleanMessages( sql ) {
        def nameToCheck
        def rows
        def uuidsToDelete = []
        def toAddressList
        sql.eachRow( QUERY_STATUS_STRING, [ 'CLEAN' ] ) { row ->
            sql.withTransaction {
                log.info "---------------------------------------------------------------------------------------\n\n"
                log.info "row['text_body'] is a ${row['text_body'].getClass().name}"
                // in the database, the "list" is one field, so it's not quite a groovy list
                toAddressList = row[ 'to_address_list' ].split( ',' )
                
                toAddressList.each { address ->
                    nameToCheck = address.replaceFirst( '@.*', '' )
                    rows = sql.rows( SELECT_USER_STRING, nameToCheck.toLowerCase() )
                    def newUUID = UUID.randomUUID()
                    if ( isNot( rows.isEmpty() ) ) {
                        sql.execute( INSERT_STRING, [ newUUID, nameToCheck, row[ 'from_address' ], address, row[ 'text_body' ], row[ 'msg_timestamp' ] ] )
                        log.info "Entered ${newUUID} into mail_store from ${row[ 'id' ]} in mail_spool_in"
                    }
                }
            }
            uuidsToDelete << row[ 'id' ]
        } // sql.eachRow
        this.updateMessageStatus( sql, uuidsToDelete, 'TRANSFERRED' )
    }
    
    def deleteTransferredMessages( sql ) {
        def uuidsToDelete = []
        sql.eachRow( QUERY_STATUS_STRING, [ 'TRANSFERRED' ] ) { row ->
            uuidsToDelete << row[ 'id' ]
        }
        if ( isNot( uuidsToDelete.isEmpty() ) ) {
            sql.withTransaction {
                sql.execute "DELETE from mail_spool_in where id in (${ uuidsToDelete.getQMarkString() })", uuidsToDelete
            }
        }

    }
    
}


