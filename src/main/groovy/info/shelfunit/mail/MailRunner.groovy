package info.shelfunit.mail

import groovy.util.ConfigSlurper

class MailRunner {
    
    static main( args ) {
        StringBuffer.metaClass.endsWith = { eString ->
            if ( delegate.length() < eString.length() ) {
                return false
            } else if ( delegate.substring( ( delegate.length() - eString.length() ), delegate.length() ).equals( eString ) ) {
                return true
            } else {
                return false
            }   
        }
        StringBuffer.metaClass.clear = { ->
            delegate.delete( 0, delegate.length() )
        }
        // def fGS = new FirstGroovyServer()
        // fGS.doStuff( Integer.parseInt( args[ 0 ] ) )
        println "in MailRunner"
        // def theURL = getClass().getResource( args[ 0 ] )
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

