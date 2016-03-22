package info.shelfunit.mail.meta

import groovy.sql.Sql

import groovy.util.logging.Slf4j 

@Slf4j
class MetaProgrammer {
    
    static runMetaProgramming() {
        ExpandoMetaClass.enableGlobally()
        runNumberMetaProgramming()
        runListMetaProgramming()
        runMapMetaProgramming()
        runSetMetaProgramming()
        MatcherMetaProgrammer.runMatcherMetaProgramming() 
        runStringBufferMetaProgramming()
        runStringMetaProgramming()
        java.sql.Timestamp.metaClass.static.create = {
            return new java.sql.Timestamp( new java.util.Date().getTime() )
        }
    }
    
    static runNumberMetaProgramming() {
        java.lang.Number.metaClass.seconds = { ->
            return delegate * 1000
        }
        java.lang.Number.metaClass.minutes = { ->
            return ( ( delegate * 1000 ) * 60 )
        }
    }
    
    static runListMetaProgramming() {
        java.util.List.metaClass.lastItem = { ->
            if ( delegate.size() != 0 ) {
                delegate.last()
            }
        }
        java.util.List.metaClass.lastSMTPCommandPrecedesMail = { ->
            delegate.last().matches( 'EHLO|HELO|RSET|AUTH' )
        }
        java.util.List.metaClass.lastSMTPCommandPrecedesRCPT = { ->
            delegate.last().matches( 'MAIL|RCPT' ) 
        }
        
        java.util.List.metaClass.includes = { i -> i in delegate 
        }
        
        java.util.List.metaClass.containsIgnoreCase = { arg ->
            def result = delegate.any { it ->
                it.toLowerCase() == arg.toLowerCase()
            }
            result
        }
        java.util.List.metaClass.getQMarkString = { arg ->
            def qMarks = []
            ( delegate.size() ).times { qMarks << '?' }
            return qMarks.join( ',' )
        }
    }
    
    static runMapMetaProgramming() {
        java.util.Map.metaClass.hasSTATInfo = { ->
            if ( ( delegate.containsKey( 'uuidList' ) ) && 
            ( delegate.containsKey( 'totalMessageSize' ) ) &&
            ( delegate.containsKey( 'timestamp' ) ) &&
            ( delegate.uuidList != null ) && ( delegate.totalMessageSize != null ) && ( delegate.timestamp != null ) 
            ) {
                return true
            } else {
                return false
            }
        }
        java.util.Map.metaClass.getSTATInfo = { Sql sql ->
            def userName = delegate.userInfo.username
            def totalSize
            def uuidList = []
            delegate.timestamp ?: java.sql.Timestamp.create() // ( new java.util.Date().getTime() )
            def rows = sql.rows( 'select sum( length( text_body ) ) from mail_store where username = ? and msg_timestamp < ?', [ userName, delegate.timestamp ] )
            if ( rows.size() != 0 ) { 
                delegate.totalMessageSize = rows[ 0 ].sum
            } 
            rows.clear()
            /*
            sql.eachRow( 'select id from mail_store where username = ?', [ userName ] ) { nextRow ->
                uuidList << nextRow.id
            }
            */
            uuidList = sql.rows( 'select id from mail_store where username = ? and msg_timestamp < ?', [ userName, delegate.timestamp ] )
            delegate.uuidList = uuidList
        }
    }
    
    static runSetMetaProgramming() {
        java.util.Set.metaClass.lastItem = { ->
            if ( delegate.size() != 0 ) {
                delegate.last()
            }
        }
        
        java.util.Set.metaClass.lastSMTPCommandPrecedesMail = { ->
            ( !delegate.isEmpty() ) && ( delegate.last().matches( 'EHLO|HELO|RSET|AUTH' ) )
        }
        java.util.Set.metaClass.lastSMTPCommandPrecedesRCPT = { ->
            ( !delegate.isEmpty() ) &&  ( delegate.last().matches( 'MAIL|RCPT' ) ) 
        }
        java.util.Set.metaClass.lastSMTPCommandPrecedesDATA = { ->
            ( !delegate.isEmpty() ) &&  (delegate.last().matches( 'RCPT' ) ) 
        }
        java.util.Set.metaClass.lastSMTPCommandPrecedesMSSG = { ->
            ( !delegate.isEmpty() ) &&  ( delegate.last().matches( 'DATA' ) ) 
        }
        java.util.Set.metaClass.includes = { i -> i in delegate 
        }
        java.util.Set.metaClass.getQMarkString = { arg ->
            def qMarks = []
            ( delegate.size() ).times { qMarks << '?' }
            return qMarks.join( ',' )
        }
        
    }
    
    static runStringBufferMetaProgramming() {
        StringBuffer.metaClass.endsWith = { end ->
            if ( delegate.length() < end.length() ) {
                return false
            } else if ( delegate.substring( ( delegate.length() - end.length() ), delegate.length() ).equals( end ) ) {
                return true
            } else {
                return false
            }   
        }
        StringBuffer.metaClass.startsWith = { start ->
            if ( delegate.length() < start.length() ) {
                return false
            } else if ( delegate.substring( 0, start.length() ).equals( start ) ) {
                return true
            } else {
                return false
            }   
        }
        StringBuffer.metaClass.clear = { ->
            delegate.delete( 0, delegate.length() )
        }
    }
    
    static runStringMetaProgramming() {
        
        String.metaClass.firstFour = { ->
            return delegate.substring( 0, 4 )
        }
        // sometimes we need to pass the string, but not the WHOLE string
        String.metaClass.firstTen = { ->
            if ( delegate.length() < 10 ) {
                return delegate
            } else {
                return delegate.substring( 0, 10 )
            }
        }
        
        String.metaClass.allButFirstFour = { ->
            return delegate.substring( 4, delegate.length() )
        }
        String.metaClass.startsWithEHLO = { ->
            return delegate.startsWith( 'EHLO' )
        }
        String.metaClass.startsWithHELO = { ->
            return delegate.startsWith( 'HELO' ) 
        }
        String.metaClass.isHelloCommand = { ->
            ( delegate.startsWithEHLO() || delegate.startsWithHELO() )
        }
        String.metaClass.getDomain = { ->
            if ( delegate.startsWithEHLO() || delegate.startsWithHELO() ) {
                return delegate.replaceFirst( 'EHLO |HELO ', '' )
            } else {
                return null
            }
        }

        String.metaClass.static.getCommandList = { -> 
            return [ 'AUTH', 'DATA',  'MAIL', // smtp
            'DELE', 'LIST', 'RCPT', 'PASS', 'RETR', 'STAT', 'USER', // pop3  
            'RSET', 'QUIT' // both  
            ]
        }
       
        // I REALLY need to come up with a better name than this
        String.metaClass.isEncapsulated = { -> 
            def returnValue = false
            
            if ( delegate.isHelloCommand() ) { 
                returnValue = true
            } 
            String.commandList.each { comm ->
                if ( delegate.startsWith( comm ) ) {
                    returnValue = true
                }
            } // this could probably be done with "find"
            returnValue
        }  // line 200
        
        // for domain in EHLOCommand
        String.metaClass.isMoreThan255Char = { ->
            delegate.length() > 255
        }
        String.metaClass.is255CharOrLess = { ->
            delegate.length() <= 255
        }
        String.metaClass.isObsoleteCommand = { ->
            if ( delegate.length() >= 4 ) {
                return delegate.firstFour().matches( "SAML|SEND|SOML|TURN" )
            } else {
                false
            }
        }
        // at some point these might be implemented
        String.metaClass.isRFC5034Command = { ->
            if ( delegate.length() >= 4 ) {
                return delegate.firstFour().matches( "CAPA|AUTH" )
            } else {
                false
            }
        }
        // at some point these might be implemented
        String.metaClass.isOptionalPostOfficeCommand = { ->
            if ( delegate.length() >= 4 ) {
                return delegate.firstFour().matches( "TOP|UIDL|APOP" )
            } else {
                false
            }
        }
        
        String.metaClass.toInt = { ->
            return Integer.parseInt( delegate )
        }
        String.metaClass.getIntInPOP3Command { ->
            Integer.parseInt( delegate.allButFirstFour().trim() )
        }
    }
} // line 231

