package info.shelfunit.mail

import groovy.util.logging.Slf4j 

import info.shelfunit.postoffice.PostOfficeActor
import info.shelfunit.postoffice.PostOfficeRunnerMessage
import info.shelfunit.postoffice.PostOfficeServer

import info.shelfunit.smtp.SMTPActor
import info.shelfunit.smtp.SMTPRunnerMessage
import info.shelfunit.smtp.SMTPServer

@Slf4j
class MailRunner {

    def withActors( def path ) {
        ConfigHolder.instance.setConfObject( path )
        def config = ConfigHolder.instance.getConfObject()
        def serverList = [ config.smtp.server.name ]

        config.smtp.other.domains.isEmpty() ?: ( serverList += config.smtp.other.domains )
        // SMTPServer smtp = new SMTPServer( serverList )
        // smtp.doStuff( config.smtp.server.port.toInteger() )
        
        def smtpActor = new SMTPActor().start()
        // sendAndPromise later?
        smtpActor.send( new SMTPRunnerMessage( serverList, config.smtp.server.port.toInteger()  ) )
        
        def poActor = new PostOfficeActor().start()
        poActor.send( new PostOfficeRunnerMessage( serverList ) )
    }
    
    def withoutActors( def path ) {
        ConfigHolder.instance.setConfObject( args[ 0 ] )
        def config = ConfigHolder.instance.getConfObject()
        def serverList = [ config.smtp.server.name ]
        config.smtp.other.domains.isEmpty() ?: ( serverList += config.smtp.other.domains )
        PostOfficeServer postO = new PostOfficeServer( serverList )
        postO.doStuff( config.postoffice.server.port.toInteger() )
        
        SMTPServer smtp = new SMTPServer( serverList )
        log.info "About to call doStuff with port ${config.smtp.server.port.toInteger()} and it's a ${config.smtp.server.port.toInteger().getClass().getName()}"
        smtp.doStuff( config.smtp.server.port.toInteger() )
    }
    
    static main( args ) {
        MetaProgrammer.runMetaProgramming()
        log.info "in MailRunner"
        def stringPath = args[ 0 ]
        ConfigHolder.instance.setConfObject( args[ 0 ] )
        def config = ConfigHolder.instance.getConfObject()
        def serverList = [ config.smtp.server.name ]

        config.smtp.other.domains.isEmpty() ?: ( serverList += config.smtp.other.domains )
        // SMTPServer smtp = new SMTPServer( serverList )
        // smtp.doStuff( config.smtp.server.port.toInteger() )
        
        def smtpActor = new SMTPActor()
        smtpActor.start()
        // sendAndPromise later?
        smtpActor.send( new SMTPRunnerMessage( serverList, config.smtp.server.port.toInteger()  ) )
        
        def poActor = new PostOfficeActor().start()
        poActor.send( new PostOfficeRunnerMessage( serverList ) )
    }
}

