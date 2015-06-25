package info.shelfunit.mail

import groovy.util.ConfigSlurper
import groovy.util.logging.Slf4j 

@Slf4j
class MailRunner {
    
    static runMetaProgramming() {
        java.util.List.metaClass.lastItem = {
            if ( delegate.size() != 0 ) {
                delegate.last()
            }
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
        StringBuffer.metaClass.startsWith = { strt ->
            if ( delegate.length() < strt.length() ) {
                return false
            } else if ( delegate.substring( 0, strt.length() ).equals( strt ) ) {
                return true
            } else {
                return false
            }   
        }
        StringBuffer.metaClass.clear = { ->
            delegate.delete( 0, delegate.length() )
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
        smtp.doStuff( 25 )
    }
}

