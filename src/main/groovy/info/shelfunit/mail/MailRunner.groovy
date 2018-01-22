package info.shelfunit.mail

import groovy.sql.Sql
import groovy.util.logging.Slf4j 

import info.shelfunit.mail.meta.MetaProgrammer

import info.shelfunit.postoffice.PostOfficeActor
import info.shelfunit.postoffice.PostOfficeRunnerMessage
import info.shelfunit.postoffice.PostOfficeServer

import info.shelfunit.spool.SpoolActor
import info.shelfunit.spool.SpoolRunnerMessage

import info.shelfunit.smtp.SMTPActor
import info.shelfunit.smtp.SMTPRunnerMessage
import info.shelfunit.smtp.SMTPServer

import info.shelfunit.spool.ClamAvClientHolder
import info.shelfunit.spool.InboundSpoolWorker

@Slf4j
class MailRunner {

    def buildServerList( def argConfig ) {
        def tempServerList = [ config.smtp.server.name ]
        config.smtp.other.domains.isEmpty() ?: ( tempServerList += config.smtp.other.domains )
        def returnList = []
        tempServerList.collect{ returnList << it.toLowerCase() }
        return returnList
    }

    def runWithActors( def path ) {
        ConfigHolder.instance.setConfObject( path )
        def config     = ConfigHolder.instance.getConfObject()
        def serverList = this.buildServerlist( config )
        log.info "here is config.watch.dir: ${config.watch.dir}"
        log.info "config.watch.dir is a ${config.watch.dir.getClass().name}"
        log.info "here is config.watch.dir.toString(): ${config.watch.dir.toString()}"
        DirectoryWatcher.init( config.watch.dir.toString() )

        config.smtp.other.domains.isEmpty() ?: ( serverList += config.smtp.other.domains )
        // SMTPServer smtp = new SMTPServer( serverList )
        // smtp.doStuff( config.smtp.server.port.toInteger() )
        
        def smtpActor    = new SMTPActor().start()
        // sendAndPromise later?
        def smtpPromise  = smtpActor.sendAndPromise( new SMTPRunnerMessage( serverList, config.smtp.server.port.toInteger() ) )
        def spoolActor   = new SpoolActor().start()
        def spoolPromise = spoolActor.sendAndPromise( new SpoolRunnerMessage( serverList, config.smtp.server.port.toInteger() ) )
        
        def poActor      = new PostOfficeActor().start()
        def poPromise    = poActor.sendAndPromise( new PostOfficeRunnerMessage( serverList ) )
        
        def keepGoing = true
        sleep( 4.seconds() )
        DirectoryWatcher.watch()
        while ( keepGoing ) {
            sleep( 60.seconds() )
            log.info "still going in runWithActors"
            /*
            def db = ConfigHolder.instance.returnDbMap()         
            def sqlObject = Sql.newInstance( db.url, db.user, db.password, db.driver )
            log.info "Starting clamAV"
            def clamAV = ClamAvClientHolder.getClamAvClient()
            log.info "Starting InboundSpoolWorker"
            def isw = new InboundSpoolWorker()
            log.info "About to run CLAM"
            isw.runClam( sqlObject, clamAV )
            log.info "About to call moveCleanMessage"
            isw.moveCleanMessages( sqlObject )
            log.info "about to call deleteTransferredMessages"
            isw.deleteTransferredMessages( sqlObject )
            */
        }
    }
    
    def runWithoutActors( def path ) {
        ConfigHolder.instance.setConfObject( path )
        def config             = ConfigHolder.instance.getConfObject()
        def serverList         = this.buildServerlist( config )
        PostOfficeServer postO = new PostOfficeServer( serverList )
        log.info "About to call doStuff with port ${config.postoffice.server.port} and it's a ${config.postoffice.server.port.getClass().getName()}"
        Thread.start( "postoffice" ) {
            postO.doStuff( config.postoffice.server.port.toInteger() )
        }
        SMTPServer smtp = new SMTPServer( serverList )
        log.info "About to call doStuff with port ${config.smtp.server.port.toInteger()} and it's a ${config.smtp.server.port.toInteger().getClass().getName()}"
        Thread.start( "smtp" ) {
            smtp.doStuff( config.smtp.server.port.toInteger() )
        }
    }
    
    static main( args ) {
        MetaProgrammer.runMetaProgramming()
        log.info "in MailRunner"
        def mRunner = new MailRunner()
        // mRunner.runWithoutActors( args[ 0 ] )
        mRunner.runWithActors( args[ 0 ] )
    }
}

