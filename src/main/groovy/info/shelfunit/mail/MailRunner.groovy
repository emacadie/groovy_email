package info.shelfunit.mail

import groovy.util.ConfigSlurper
import groovy.util.logging.Slf4j 
import groovy.sql.Sql

@Slf4j
class MailRunner {
    
    static runMetaProgramming() {
        ExpandoMetaClass.enableGlobally()
        java.util.List.metaClass.lastItem = { ->
            if ( delegate.size() != 0 ) {
                delegate.last()
            }
        }
        java.util.Set.metaClass.lastItem = { ->
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
        
        java.util.Set.metaClass.lastCommandPrecedesMail = { ->
            delegate.last().matches( 'EHLO|HELO|RSET' )
        }
        java.util.Set.metaClass.lastCommandPrecedesRCPT = { ->
            delegate.last().matches( 'MAIL|RCPT' ) 
        }
        java.util.Set.metaClass.includes = { i -> i in delegate 
        }
        
        java.util.regex.Matcher.metaClass.extractUserName = { ->
            delegate[ 0 ][ 2 ].substring( 0, ( delegate[ 0 ][ 2 ].length() - ( delegate[ 0 ][ 3 ].length() + 1 ) ) )
        }
        java.util.regex.Matcher.metaClass.getEmailAddress = { ->
            delegate[ 0 ][ 2 ]
        }
        java.util.regex.Matcher.metaClass.extractDomain = { ->
            delegate[ 0 ][ 3 ]
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
        def db = [ url: "jdbc:postgresql://${System.properties[ 'host_and_port' ]}/${System.properties[ 'dbname' ]}",
        user: System.properties[ 'dbuser' ], password: System.properties[ 'dbpassword' ], driver: 'org.postgresql.Driver' ]
        def sql = Sql.newInstance( db.url, db.user, db.password, db.driver )
        def config = new ConfigSlurper().parse( new File( args[ 0 ] ).toURL() )
        def serverName = config.smtp.server.name
        log.info "Here is config.smtp.server.name: ${config.smtp.server.name}"
        def serverList = [ config.smtp.server.name ]
        config.smtp.other.domains.isEmpty() ?: ( serverList += config.smtp.other.domains )
        SMTPServer smtp = new SMTPServer( serverList )
        smtp.doStuff( config.smtp.server.port.toInteger(), sql )
    }
}

