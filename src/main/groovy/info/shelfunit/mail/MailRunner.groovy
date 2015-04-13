package info.shelfunit.mail

class MailRunner {
    
    static main( args ) {
        // def fGS = new FirstGroovyServer()
        // fGS.doStuff( Integer.parseInt( args[ 0 ] ) )
        SMTPServer smtp = new SMTPServer()
        smtp.doStuff( 25 )
    }
}

