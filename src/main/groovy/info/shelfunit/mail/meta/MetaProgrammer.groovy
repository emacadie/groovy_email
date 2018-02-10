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
        runStringBuilderMetaProgramming()
        StringMetaProgrammer.runStringMetaProgramming()
        java.sql.Timestamp.metaClass.static.create = {
            return new java.sql.Timestamp( new java.util.Date().getTime() )
        }
        runOutputStreamMetaProgramming()
        runJavaObjectMetaProgramming()
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
        // groovy lists have ".last()" - perhaps I don't need this method - or perhaps this is for null checking
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
        // java.util.List has "contains" - do I need this?
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
        java.util.Map.metaClass.getSTATInfo = { Sql sqlObject ->
            def userNameLC = delegate.userInfo.username.toLowerCase()
            def totalSize
            def uuidList   = []
            delegate.timestamp ?: java.sql.Timestamp.create() // ( new java.util.Date().getTime() )
            def rows = sqlObject.rows( 
                'select sum( length( text_body ) ) from mail_store where username_lc = ? and msg_timestamp < ?', 
                [ userNameLC, delegate.timestamp ] 
            )
            if ( rows.size() != 0 ) { 
                delegate.totalMessageSize = rows[ 0 ].sum
            } 
            rows.clear()
            
            uuidList = sqlObject.rows( 'select id from mail_store where username_lc = ? and msg_timestamp < ?', [ userNameLC, delegate.timestamp ] )
            delegate.uuidList = uuidList
        }
        
        java.util.Map.metaClass.addDomainToOutboundMap = { String domain ->
            if ( !delegate.containsKey( domain ) ) {
                delegate[ domain ] = [ ] 
            }
        }

        java.util.Map.metaClass.addToListInMap = { Object key, Object value ->
            if ( !delegate.containsKey( key ) ) { delegate[ key ] = [] }
                        delegate[ key ] << value
        }
        
    }
    
    static runSetMetaProgramming() {
        // null checking
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
    
    static runStringBuilderMetaProgramming() {
        StringBuilder.metaClass.endsWith = { end ->
            if ( delegate.length() < end.length() ) {
                return false
            } else if ( delegate.substring( ( delegate.length() - end.length() ), delegate.length() ) == end  ) {
                return true
            } else {
                return false
            }   
        }
        StringBuilder.metaClass.startsWith = { start ->
            if ( delegate.length() < start.length() ) {
                return false
            } else if ( delegate.substring( 0, start.length() ) == start  ) {
                return true
            } else {
                return false
            }   
        }
        StringBuilder.metaClass.clear = { ->
            delegate.delete( 0, delegate.length() )
        }
    }

    static runOutputStreamMetaProgramming() {
        java.io.OutputStream.metaClass.send = { arg ->
            delegate << arg
            delegate.flush()
        }
    }
    
    static runJavaObjectMetaProgramming() {
        /**
        I decided to use metaprogramming to do something other that ! to mean "not".
        The issue is that in Groovy, "not" is already taken in groovy.xml.Entity and org.codehaus.groovy.ast.builder.AstSpecificationCompiler. 
        So I tried isNot, doesNot, doNot, haveNot. It was kind of awkward. 
        So I just decided to add a "_" in front of "not" and be done with it.
        */
        java.lang.Object.metaClass.static._not = { boolean arg ->
            if ( arg == true ) { 
                return false 
            } else { 
                return true 
            }
        }
        
    }
} // line 231

