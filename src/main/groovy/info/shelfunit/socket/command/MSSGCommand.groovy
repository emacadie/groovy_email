package info.shelfunit.socket.command

import groovy.sql.Sql
import groovy.util.logging.Slf4j 

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
            uuidSet << UUID.randomUUID().toString()
        }
        def resultStringList = []
        bufferMap.forwardPath.each { user ->
        }
        def regexResult = ( theMessage ==~ pattern )
        def q = theMessage =~ pattern
        if ( !prevCommandSet.lastCommandPrecedesRCPT() ) {
            resultMap.resultString = "503 Bad sequence of commands"
        } else if ( !theMessage.startsWith( 'RCPT TO:' ) ) {
            resultMap.resultString = "501 Command not in proper form"
        } else if ( !regexResult ) {
            resultMap.resultString = "501 Command not in proper form"
        } else if ( !( theMessage ==~ pattern ) ) {
            resultMap.resultString = "501 Command not in proper form"
        } else if ( !domainList.includes( q.extractDomain() ) ) {
            resultMap.resultString = "550 No such user"
        } else {
            
            // log.info "Here is q[ 0 ][ 2 ]: ${q[0][2]}, Here is q[ 0 ][ 3 ]: ${q[0][3]}"
            def userName = q.extractUserName() 
            // log.info "here is userName: ${userName}"
            def rows = sql.rows( 'select * from email_user where username=?', userName )
            // log.info "here is rows?.size() : ${rows?.size()} "
            if ( rows.size() != 0 ) { // row?.size() != null ) { //  != 0 ) {
                bufferMap.forwardPath << q.getEmailAddress() // q[ 0 ][ 2 ]
                resultMap.resultString = '250 OK'
                prevCommandSet << 'RCPT'
            } else {
                resultMap.resultString = "550 No such user"
            }
            
        }
        resultMap.bufferMap = bufferMap
        resultMap.prevCommandSet = prevCommandSet

		log.info "here is resultMap: ${resultMap.toString()}"
		resultMap
    } // process
    
    def addMessageToDatabase( theMessage, bufferMap, uuidSet ) {
        def result = '250 OK'
        def toAddresses = bufferMap.forwardPath
        def fromAddress = bufferMap.reversePath
        def insertCounts 
        try {
            sql.withTransaction {
                toAddresses.eachWithIndex { address, i ->
                    def q = address =~ regex
                    def wholeAddress = q[ 0 ][ 1 ]
                    def userName = q[ 0 ][ 2 ]
                    mail_store
                    
                    insertCounts = sql.withBatch( 'insert into mail_store(id, username, from_address, to_address, message) values (?, ?, ?, ?, ?)' ) { stmt ->
                        ps.addBatch( [ uuidSet[ i ], userName, reversePath, wholeAddress, theMessage ] )
                    }
                }
            }
        } catch ( Exception e ) {
            result = '500 Something went wrong'
        }
        result
    } // addMessageToDatabase
}

