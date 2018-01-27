package info.shelfunit.spool

import groovy.util.logging.Slf4j 
import java.sql.SQLException
import fi.solita.clamav.ClamAVClient

@Slf4j
class InboundSpoolWorker{
    
    // states of message: ENTERED, CLEAN (clean CalmAV scan) or UNCLEAN (unclean ClamAV scan)
    // TRANSFERRED: a clean message has been copied to mail_store for each user listed in the message
    
    ClamAVClient clamavj
    static final QUERY_SPOOL_STATUS          = 'select * from mail_spool_in where status_string = ?'
    static final INSERT_INTO_MAIL_STORE      = "insert into mail_store( id, username, username_lc, " +
        "from_address, to_address, text_body, msg_timestamp ) values ( ?, ?, ?, ?, ?, ?, ? )"
    static final INSERT_INTO_BAD_MAIL_STORE  = "insert into bad_mail_store( id, username, username_lc, " +
        "from_address, to_address, text_body, msg_timestamp ) values ( ?, ?, ?, ?, ?, ?, ? )"
    static final SELECT_USER_STRING          = 'select username_lc from email_user where username_lc = ?'
    
    InboundSpoolWorker( ) {
    }
    
    def runClam( sqlObject, clamavj ) {
        def cleanUUIDs   = []
        def uncleanUUIDs = []
        sqlObject.eachRow( QUERY_SPOOL_STATUS, [ 'ENTERED' ] ) { row ->
            byte[] data       = row[ 'text_body' ].getBytes()
            InputStream input = new ByteArrayInputStream( data )
            log.info "input is a ${input.getClass().name}"
            def isClean = this.runClamOnMessage( input, clamavj )
            if ( isClean ) {
                cleanUUIDs << row[ 'id' ]
            } else {
                uncleanUUIDs << row[ 'id' ]
            }
        } // sqlObject.eachRow
       this.updateMessageStatus( sqlObject, cleanUUIDs, 'CLEAN' )
       if ( _not( uncleanUUIDs.isEmpty() ) ) { 
           this.updateMessageStatus( sqlObject, uncleanUUIDs, 'UNCLEAN' ) 
       }
    } // runClam
    
    def updateMessageStatus( sqlObject, uuidList, status ) {
        try {
            log.info "here is idsToDelete: ${uuidList} and it is a ${uuidList.getClass().name}"
            def insertCounts 
            def params    = []
            def newObject = uuidList.plus( 0, status )
            log.info "newObject is a ${newObject.getClass().name}, here it is: ${newObject}"
            if ( _not( uuidList.isEmpty() ) ) { 
                sqlObject.withTransaction {
                    params << status
                    params += uuidList // you can do this, or UUIDs.plus( 0, status ) which adds status to front of list
                    sqlObject.execute( 
                        "UPDATE mail_spool_in set status_string = ? " +
                            "where id in (${uuidList.getQMarkString()}) ", 
                        uuidList.plus( 0, status ) 
                    )
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
        if ( _not( ClamAVClient.isCleanReply( reply ) ) ) {
            log.info "aaargh. Something was found"
            messageIsClean = false
        }
        return messageIsClean
    }
    
    def moveCleanMessages( sqlObject ) {
        def nameToCheck
        def rows
        def uuidsToTransfer   = []
        def uuidsInvalidUsers = []
        def toAddressList
        def theUUID
        sqlObject.eachRow( QUERY_SPOOL_STATUS, [ 'CLEAN' ] ) { row ->
            sqlObject.withTransaction {
                log.info "---------------------------------------------------------------------------------------\n\n"
                log.info "row['text_body'] is a ${row['text_body'].getClass().name}"
                // in the database, the "list" is one field, so it's not quite a groovy list
                toAddressList = row[ 'to_address_list' ].split( ',' )
                def newUUID
                
                toAddressList.each { address ->
                    nameToCheck = address.replaceFirst( '@.*', '' )
                    log.info "Here is nameToCheck: ${nameToCheck}"
                    rows = sqlObject.rows( SELECT_USER_STRING, nameToCheck.toLowerCase() )
                    println "Here is row[ 'id' ]: ${row[ 'id' ]}"
                    theUUID = row[ 'id' ]
                    if ( _not( rows.isEmpty() ) ) {
                        newUUID = UUID.randomUUID()
                        println "Here is newUUID: ${newUUID}"
                        sqlObject.execute( INSERT_INTO_MAIL_STORE, 
                             [ newUUID,                   // id
                               nameToCheck,               // username
                               nameToCheck.toLowerCase(), // username_lc
                               row[ 'from_address' ],     // "from_address
                               address,                   // to_address
                               row[ 'text_body' ],        // text_body 
                               row[ 'msg_timestamp' ]     // msg_timestamp
                             ] 
                        )
                        log.info "Entered ${newUUID} into mail_store from ${row[ 'id' ]} in mail_spool_in"
                        uuidsToTransfer << row[ 'id' ]
                    } else {
                        newUUID = UUID.randomUUID()
                        println "Here is newUUID: ${newUUID}"
                        sqlObject.execute( INSERT_INTO_BAD_MAIL_STORE, 
                             [ newUUID,                   // id
                               nameToCheck,               // username
                               nameToCheck.toLowerCase(), // username_lc
                               row[ 'from_address' ],     // "from_address
                               address,                   // to_address
                               row[ 'text_body' ],        // text_body 
                               row[ 'msg_timestamp' ]     // msg_timestamp
                             ] 
                        )
                        log.info "Entered ${newUUID} into bad_mail_store from ${row[ 'id' ]} in mail_spool_in"
                        uuidsInvalidUsers << theUUID // row[ 'id ']
                    } // add a list of uuids for invalid users here
                } // toAddressList.each
            }
            
        } // sqlObject.eachRow
        this.updateMessageStatus( sqlObject, uuidsToTransfer,   'TRANSFERRED' )
        this.updateMessageStatus( sqlObject, uuidsInvalidUsers, 'INBOUND_INVALID_USER' )
    }
    
    def deleteTransferredMessages( sqlObject ) {
        this.deleteMessages( sqlObject, 'TRANSFERRED' )
    }

    def deleteInvalidUserMessages( sqlObject ) {
        this.deleteMessages( sqlObject, 'INBOUND_INVALID_USER' )
    }
    
    def deleteUncleanMessages( sqlObject ) {
        this.deleteMessages( sqlObject, 'UNCLEAN' )
    }
    
    private deleteMessages( sqlObject, status ) {
        def uuidsToDelete = []
        sqlObject.eachRow( QUERY_SPOOL_STATUS, [ status ] ) { row ->
            uuidsToDelete << row[ 'id' ]
        }
        if ( _not( uuidsToDelete.isEmpty() ) ) {
            sqlObject.withTransaction {
                sqlObject.execute "DELETE from mail_spool_in where id in (${ uuidsToDelete.getQMarkString() })", uuidsToDelete
            }
        }
    }
    
}


