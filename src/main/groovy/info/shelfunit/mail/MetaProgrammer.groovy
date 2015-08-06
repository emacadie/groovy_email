package info.shelfunit.mail

import groovy.util.logging.Slf4j 

@Slf4j
class MetaProgrammer {
    
    static runMetaProgramming() {
        ExpandoMetaClass.enableGlobally()
        runListMetaProgramming()
        runSetMetaProgramming()
        runMatcherMetaProgramming() 
        runStringBufferMetaProgramming()
        runStringMetaProgramming()
    }
    
    static runListMetaProgramming() {
        java.util.List.metaClass.lastItem = { ->
            if ( delegate.size() != 0 ) {
                delegate.last()
            }
        }
        java.util.List.metaClass.lastCommandPrecedesMail = { ->
            delegate.last().matches( 'EHLO|HELO|RSET' )
        }
        java.util.List.metaClass.lastCommandPrecedesRCPT = { ->
            delegate.last().matches( 'MAIL|RCPT' ) 
        }
        java.util.List.metaClass.includes = { i -> i in delegate 
        }
    }
    
    static runSetMetaProgramming() {
        java.util.Set.metaClass.lastItem = { ->
            if ( delegate.size() != 0 ) {
                delegate.last()
            }
        }
        java.util.Set.metaClass.lastCommandPrecedesMail = { ->
            delegate.last().matches( 'EHLO|HELO|RSET' )
        }
        java.util.Set.metaClass.lastCommandPrecedesRCPT = { ->
            delegate.last().matches( 'MAIL|RCPT' ) 
        }
        java.util.Set.metaClass.lastCommandPrecedesDATA = { ->
            delegate.last().matches( 'RCPT' ) 
        }
        java.util.Set.metaClass.includes = { i -> i in delegate 
        }
    }
    
    static runMatcherMetaProgramming() {
        java.util.regex.Matcher.metaClass.extractUserName = { ->
            delegate[ 0 ][ 2 ].substring( 0, ( delegate[ 0 ][ 2 ].length() - ( delegate[ 0 ][ 3 ].length() + 1 ) ) )
        }
        java.util.regex.Matcher.metaClass.getEmailAddress = { ->
            delegate[ 0 ][ 2 ]
        }
        java.util.regex.Matcher.metaClass.extractDomain = { ->
            delegate[ 0 ][ 3 ]
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
        // I REALLY need to come up with a better name than this
        String.metaClass.isEncapsulated = {
            def returnValue = false
            if ( delegate.isHelloCommand() ) { 
                returnValue = true
            } else if ( delegate.startsWith( 'MAIL' ) ) {
                returnValue = true
            } else if ( delegate.startsWith( 'RCPT' ) ) {
                returnValue = true
            } else if ( delegate.startsWith( 'RSET' ) ) {
                returnValue = true
            } else if ( delegate.startsWith( 'DATA' ) ) {
                returnValue = true
            }
            returnValue
        }
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
    }
}

