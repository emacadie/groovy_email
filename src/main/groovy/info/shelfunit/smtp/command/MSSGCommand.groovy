package info.shelfunit.smtp.command

import groovy.sql.Sql
import groovy.util.logging.Slf4j 

import java.sql.SQLException

import visibility.Hidden

@Slf4j
class MSSGCommand {
    
    @Hidden def uuidSet
/*    
    static regex = '''([\\w!#$%&’*+/=?`{|}~^-]+(?:\\.[\\w!#$%&’ # WTF?
*+/=?`{|}~^-]+)*@((?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}))$(?x)'''
*/
   static regex = '''(([\\w!#$%&’*+/=?`{|}~^-]+(?:\\.[\\w!#$%&’ # WTF?
*+/=?`{|}~^-]+)*)@((?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}))$(?x)'''    
/*
groovy:001> *+/=?`{|}~^-]+)*)@((?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}))$(?x)''' 
===> (([\w!#$%&’*+/=?`{|}~^-]+(?:\.[\w!#$%&’ # WTF?
*+/=?`{|}~^-]+)*)@((?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,6}))$(?x)
groovy:000> q = address =~ regex
===> java.util.regex.Matcher[pattern=(([\w!#$%&’*+/=?`{|}~^-]+(?:\.[\w!#$%&’ # WTF?
*+/=?`{|}~^-]+)*)@((?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,6}))$(?x) region=0,24 lastmatch=]
groovy:000> q[0][2]
===> jack.oneill
groovy:000> q[0][1]
===> jack.oneill@stargate.mil
groovy:000> q[0][3]
===> stargate.mil
*/
    @Hidden Sql sql
    @Hidden List domainList
    MSSGCommand( def argSql, def argDomainList ) {
        log.info "Starting new MSSGCommand"
        println "Here is argDomainList: ${argDomainList}"
        this.sql = argSql
        this.domainList = argDomainList
        uuidSet = [] as Set
    }
    
    /*
    myList = [ 1, 2, 3, 4 ]
for ( item in myList ) {
   println item
   if ( item == 2 ) { break }
}

myList = [ 1, 2, 3, 4 ]
for ( item in myList ) {
   println item
   if ( item == 2 ) { break }
}

1.upto(4) {
   println "Test ${it}"
   if (it == 2) {break}
}
    */
    
    def process( theMessage, prevCommandSet, bufferMap ) {
        log.info "In MSSGCommand"
        def resultString
        def resultMap = [:]
        resultMap.clear()
        
        bufferMap.forwardPath.size().times() {
            uuidSet << UUID.randomUUID() 
        }

        if ( !prevCommandSet.lastCommandPrecedesMSSG() ) {
            resultMap.resultString = "503 Bad sequence of commands"
        } else {
            resultMap.resultString =  this.addMessageToDatabase( theMessage, bufferMap, uuidSet )
        }
        if ( resultMap.resultString == '250 OK' ) {
            resultMap.bufferMap = [:]
        }
        resultMap.prevCommandSet = prevCommandSet

		log.info "here is resultMap: ${resultMap.toString()}"
		resultMap
    } // process
    
    def addMessageToDatabase( theMessage, bufferMap, uuidSet ) {
        log.info "log is a ${log.class.name}"
        def result = '250 OK'
        def toAddresses = bufferMap.forwardPath
        def fromAddress = bufferMap.reversePath
        def insertCounts 
        try {
            sql.withTransaction {
                toAddresses.eachWithIndex { address, i ->
                    def q = address =~ regex
                    def wholeAddress = q.getWholeAddressInMSSG()
                    def userName = q.getUserNameInMSSG()
                    log.info "here are the args: [ uuidSet[ i ]: ${uuidSet[ i ]}, userName: ${userName}, fromAddress: ${fromAddress}, wholeAddress: ${wholeAddress}, theMessage: ${theMessage}"
                    insertCounts = sql.withBatch( 'insert into mail_store(id, username, from_address, to_address, text_body) values (?, ?, ?, ?, ?)' ) { stmt ->
                        log.info "uuidSet[ i ] is a ${uuidSet[ i ].class.name}"
                        log.info "stmt is a ${stmt.class.name}"
                        // stmt.setObject( 1, uuidSet[ i ] )
                        // stmt.setBlob( 5, theMessage )
                        stmt.addBatch( [ uuidSet[ i ], userName, fromAddress, wholeAddress, theMessage ] )
                    }
                }
            }
        } catch ( Exception e ) {
            result = '500 Something went wrong'
            SQLException ex = e.getNextException()
            log.info "Next exception message: ${ex.getMessage()}"
            // ex.printStrackTrace()
            log.error "something went wrong", ex 
            // log.error "Failed to format {}", result, ex
        }
        println "here is insertCounts: ${insertCounts}"
        result
    } // addMessageToDatabase
}
