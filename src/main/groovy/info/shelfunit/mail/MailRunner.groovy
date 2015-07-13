package info.shelfunit.mail

import groovy.util.ConfigSlurper
import groovy.util.logging.Slf4j 

@Slf4j
class MailRunner {
    
    static runMetaProgramming() {
        ExpandoMetaClass.enableGlobally()
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
        String.metaClass.firstFour = { ->
            return delegate.substring( 0, 4 )
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
        
        // for domain in EHLOCommand
        String.metaClass.isMoreThan255Char = { ->
            delegate.length() > 255
        }
        String.metaClass.is255CharOrLess = { ->
            delegate.length() <= 255
        }
        String.metaClass.isObsoleteCommand = { ->
            delegate.firstFour().matches( "SAML|SEND|SOML|TURN" )
        }
    }
    
    static main( args ) {
        MailRunner.runMetaProgramming()
        log.info "in MailRunner"
        URL theURL = getClass().getResource( "/log4j.properties" );
        log.info "theURL is a ${theURL.class.name}"
        def config = new ConfigSlurper().parse( new File( args[ 0 ] ).toURL() )
        def serverName = config.smtp.server.name
        log.info "Here is config.smtp.server.name: ${config.smtp.server.name}"
        SMTPServer smtp = new SMTPServer( serverName )
        smtp.doStuff( config.smtp.server.port.toInteger() )
    }
}

