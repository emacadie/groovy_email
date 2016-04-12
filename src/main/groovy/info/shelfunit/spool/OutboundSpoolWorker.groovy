package info.shelfunit.spool

import groovy.util.logging.Slf4j 
import info.shelfunit.mail.ConfigHolder
import java.io.IOException
import java.sql.SQLException
// import java.util.concurrent.ConcurrentHashMap
import fi.solita.clamav.ClamAVClient

@Slf4j
class OutboundSpoolWorker {
    
    static localPart  = '''(([\\w!#$%&’*+/=?`{|}~^-]+(?:\\.[\\w!#$%&’*+/=?`{|}~^-]+)*)@'''
    static domainName = '''((?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}))'''
    static regex = localPart + domainName  
    
    // states of message: ENTERED, CLEAN (clean CalmAV scan) or UNCLEAN (unclean ClamAV scan)
    // TRANSFERRED: a clean message has been copied to mail_store for each user listed in the message
    
    final config
    final sql
    ClamAVClient clamavj
    static QUERY_STATUS_STRING = 'select * from mail_spool_out where status_string = ?'
    static INSERT_STRING = 'insert into mail_store( id, username, from_address, to_address, text_body, msg_timestamp ) values ( ?, ?, ?, ?, ?, ? )'
    static SELECT_USER_STRING = 'select username from email_user where lower( username )=?'
    
    OutboundSpoolWorker( ) {
    }
    
    def runClam( sql, clamavj ) {
        def cleanUUIDs = []
        def uncleanUUIDs = []
        sql.eachRow( QUERY_STATUS_STRING, [ 'ENTERED' ] ) { row ->
            byte[] data = row[ 'text_body' ].getBytes()
            InputStream input = new ByteArrayInputStream( data )
            println "input is a ${input.getClass().name}"
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
    
    def updateMessageStatus( sql, UUIDs, status ) {
        try {
            log.info "here is uuid list: ${UUIDs} and it is a ${UUIDs.getClass().name} status is ${status}"
            def insertCounts 
            def params = []
            def newObject = UUIDs.plus( 0, status )
            log.info "newObject is a ${newObject.getClass().name}, here it is: ${newObject}"
            if ( isNot( UUIDs.isEmpty() ) ) { 
                
                sql.withTransaction {
                    params << status
                    params += UUIDs // you can do this, or UUIDs.plus( 0, status ) which adds status to front of list
                    sql.execute( "UPDATE mail_spool_out set status_string = ? where id in (${UUIDs.getQMarkString()}) ", UUIDs.plus( 0, status ) )
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
    
    def deliverMessages( sql, domainList, outgoingPort ) {
        try {
        log.info "starting deliverMessages"
        def nameToCheck
        def rows
        def uuidsToDelete = []
        def toAddressList
        log.info "About to call query ${QUERY_STATUS_STRING}"
        sql.eachRow( QUERY_STATUS_STRING, [ 'CLEAN' ] ) { row ->
            // sql.withTransaction {
                log.info "---------------------------------------------------------------------------------------\n\n"
                log.info "row['text_body'] is a ${row['text_body'].getClass().name}"
                // in the database, the "list" is one field, so it's not quite a groovy list
                toAddressList = row[ 'to_address_list' ].split( ',' )
                def outgoingMap = [:]
                toAddressList.each { address ->
                    log.info "Here is addr: ${address}"
                    def q = address =~ regex
                    log.info "here is q[ 0 ][ 0 ]: ${q[ 0 ][ 0 ]}"
                    log.info "here is q[ 0 ][ 1 ]: ${q[ 0 ][ 1 ]}"
                    log.info "here is q[ 0 ][ 2 ]: ${q[ 0 ][ 2 ]}"
                    log.info "here is q[ 0 ][ 3 ]: ${q[ 0 ][ 3 ]}"
                    q.each { match ->
                        match.eachWithIndex { group, n ->
                            log.info "${n}, <$group>"
                        }
                    }
                    outgoingMap.addDomainToOutboundMap( q.getDomainInOutboundSpool() )
                    outgoingMap[ q.getDomainInOutboundSpool() ] << q.getUserInOutboundSpool()
                    println "------"
                }
                log.info "Here is outgoingMap: ${outgoingMap.toString()}"
                log.info "About to check internal domains"
                // go through, see if any messages are to anyone in this domain
                outgoingMap.each { k, v ->
                    if ( domainList.contains( k ) ) {
                        sql.withTransaction {
                            def userList = v
                            useList.each { user ->
                                sql.execute "insert into mail_store( id, username, from_address, to_address, text_body, msg_timestamp ) values ( ?, ?, ?, ?, ?, ? )", [ UUID.randomUUID(), user, row[ 'from_address' ], user + '@' + k , row[ 'text_body' ], row[ 'msg_timestamp' ] ]
                            }
                        }
                        outgoingMap.remove( k )
                        log.info "Key ${k} has value ${v}"
                    }
                    log.info "++++"
                    
                }
                log.info "About to check external domains, Here is outgoingMap: ${outgoingMap.toString()}"
                // go through, see if any messages are to anyone not in this domain
                outgoingMap.each { otherDomain, otherUserList ->
                    log.info "Looking at otherDomain ${otherDomain} with list ${otherUserList}"
                    if ( doesNot( domainList.contains( otherDomain ) ) ) {
                        // def socket = new Socket( otherDomain, String.toInt( outgoingPort ) )
                        // def socket = new Socket( otherDomain, outgoingPort )
                        def sr = new SocketRetriever( otherDomain.toString(), outgoingPort ) 
                        def socket = sr.getSocket() 
                        // def socket = new Socket( otherDomain, outgoingPort )
                        socket.setSoTimeout( 10.minutes() )
                        socket.withStreams { input, output ->
                            def mSender = new MessageSender()
                            mSender.doWork( input, output, row, otherDomain, otherUserList, domainList[ 0 ] )
                            
                        }
                    }
                }
                outgoingMap.clear()
                /*
                toAddressList.each { address ->
                    nameToCheck = address.replaceFirst( '@.*', '' )
                    rows = sql.rows( SELECT_USER_STRING, nameToCheck.toLowerCase() )
                    def newUUID = UUID.randomUUID()
                    if ( isNot( rows.isEmpty() ) ) {
                        sql.execute( INSERT_STRING, [ newUUID, nameToCheck, row[ 'from_address' ], address, row[ 'text_body' ], row[ 'msg_timestamp' ] ] )
                        log.info "Entered ${newUUID} into mail_store from ${row[ 'id' ]} in mail_spool_out"
                    }
                }
                */
            // } // sql.withTransaction
            uuidsToDelete << row[ 'id' ]
        } // sql.eachRow
        
        this.updateMessageStatus( sql, uuidsToDelete, 'TRANSFERRED' )
        } catch ( Exception e ) {
            log.error "Exception ${e.getClass().name}", e
        }
    }
    
    
    def deleteTransferredMessages( sql ) {
        def uuidsToDelete = []
        sql.eachRow( QUERY_STATUS_STRING, [ 'TRANSFERRED' ] ) { row ->
            uuidsToDelete << row[ 'id' ]
        }
        if ( isNot( uuidsToDelete.isEmpty() ) ) {
            sql.withTransaction {
                sql.execute "DELETE from mail_spool_out where id in (${ uuidsToDelete.getQMarkString() })", uuidsToDelete
            }
        }

    }
    
}


