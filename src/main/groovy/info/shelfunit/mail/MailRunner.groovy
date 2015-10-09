package info.shelfunit.mail

import groovy.util.logging.Slf4j 
import info.shelfunit.smtp.SMTPServer
       
import info.shelfunit.postoffice.PostOfficeActor
import info.shelfunit.postoffice.PostOfficeRunnerMessage
import info.shelfunit.smtp.SMTPActor
import info.shelfunit.smtp.SMTPRunnerMessage

@Slf4j
class MailRunner {

    static main( args ) {
        MetaProgrammer.runMetaProgramming()
        log.info "in MailRunner"
        ConfigHolder.instance.setConfObject( args[ 0 ] )
        def config = ConfigHolder.instance.getConfObject()
        def serverList = [ config.smtp.server.name ]

        config.smtp.other.domains.isEmpty() ?: ( serverList += config.smtp.other.domains )
        // SMTPServer smtp = new SMTPServer( serverList )
        // smtp.doStuff( config.smtp.server.port.toInteger() )
        
        def smtpActor = new SMTPActor().start()
        // sendAndPromise later?
        smtpActor.send( new SMTPRunnerMessage( serverList ) )
        
        def poActor = new PostOfficeActor().start()
        poActor.send( new PostOfficeRunnerMessage( serverList ) )
    }
}

