package info.shelfunit.mail

import groovy.util.ConfigSlurper

class MailRunner {
    
    static main( args ) {
        StringBuffer.metaClass.endsWith = { end ->
            if ( delegate.length() < end.length() ) {
                return false
            } else if ( delegate.substring( ( delegate.length() - end.length() ), delegate.length() ).equals( end ) ) {
                return true
            } else {
                return false
            }   
        }
        StringBuffer.metaClass.clear = { ->
            delegate.delete( 0, delegate.length() )
        }
        println "in MailRunner"
        URL theURL = getClass().getResource( "/log4j.properties" );
        println "theURL is a ${theURL.class.name}"
        def config = new ConfigSlurper().parse( new File( args[ 0 ] ).toURL() )
        def stuff = config.smtp.server.name
        println "Here is config.smtp.server.name: ${config.smtp.server.name}"
        println "Here is stuff: ${stuff} and it's a ${stuff.class.name}"
        SMTPServer smtp = new SMTPServer( stuff )
        smtp.doStuff( 25 )
    }
}

