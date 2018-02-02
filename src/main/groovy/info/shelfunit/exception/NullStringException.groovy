package info.shelfunit.exception

class NullStringException extends Exception {
    // from https://stackoverflow.com/questions/21706722/fetch-only-first-n-lines-of-a-stack-trace
    def printReducedStackTrace( String packageName ) {
        StringWriter sw  = new StringWriter()
        PrintWriter pw   = new PrintWriter( sw )
        StringBuilder sb = new StringBuilder( "\n" )
        this.printStackTrace( pw )
        String[] splitted = sw.toString().split( "\n" )
        
        splitted.each { nextLine ->
            if ( nextLine.contains( packageName ) ) {
                sb << nextLine.trim() 
                sb << "\n"
            }
        }
        sw.close()
        pw.close()
        return sb.toString()
    }
}
