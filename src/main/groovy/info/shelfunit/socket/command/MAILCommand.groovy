package info.shelfunit.socket.command


import groovy.util.logging.Slf4j 
// reg ex for email from http://www.mkyong.com/regular-expressions/how-to-validate-email-address-with-regular-expression/
// look at http://www.ngdc.noaa.gov/wiki/index.php?title=Regular_Expressions_in_Groovy as well
// RFC 5321 4.1.1 The reverse-path is the argument of the MAIL command, the forward-path is the argument of
//   the RCPT command, and the mail data is the argument of the DATA command. 

@Slf4j
class MAILCommand {
    
    // http://howtodoinjava.com/2014/11/11/java-regex-validate-email-address/
    static regex = '''^(MAIL FROM):<[\\w!#$%&’*+/=?`{|}~^-]+(?:\\.[\\w!#$%&’ # WTF?
*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}>$(?x)'''
/*
regexB = '''^(MAIL FROM):<[\\w!#$%&’*+/=?`{|}~^-]+(?:\\.[\\w!#$%&’*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}>$'''
*/
    static pattern = ~regex
    
    def resultMap = [:]
    def process( theMessage, prevCommandList ) {
        def resultString
        // if () { }
        
    }
    
    def process( theMessage, prevCommandList, bufferMap ) {
        def resultString
        resultMap.clear()
        bufferMap.reversePath = '' 
        bufferMap.forwardPath = ''
        bufferMap.mailData = ''
        
        if ( !theMessage.startsWith( 'MAIL FROM:' ) ) {
            resultMap.resultString = "501 Command not in proper form"
        } else if ( !prevCommandList.last.matches( 'EHLO|HELO|RSET' ) ) {
            resultMap.resultString = "503 Bad sequence of commands"
        } else if ( !( theMessage ==~ pattern) ) {
            resultMap.resultString = "501 Command not in proper form"
        } else {
            prevCommandList << 'MAIL'
            resultMap.prevCommandList = prevCommandList
            resultMap.resultString = '250 OK'
            def q = theMessage ==~ patterm
            bufferMap.reversePath = q[0][1] + "@" + q[0][2]
            resultMap.bufferMap = bufferMap
        }
        /*
pattern = ~/^.*?groovy.*$/
===> ^.*?groovy.*$
pattern.class.name
===> java.util.regex.Pattern
input = 'i love me some groovy code'
input ==~ pattern
===> true
groovy:000> input =~ pattern
mPattern = ~/MAIL\sFROM:<\s[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})>$/
                                                [_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})
mP = ~/^MAIL FROM:<(.+)@(.+)>$/

~/^(MAIL FROM:)<(.+)@(.+)>$/
~/^(MAIL FROM:)<(.+)@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})>$/

~/^(MAIL FROM:)<[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})?$/

~/^(MAIL FROM:)<[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})>$/
q = mm =~ mP // get the matcher as q
to view the matches
q[0][1]
I have no idea why you need a 2-dimensional array
~/^(MAIL FROM:)<(.+)@(.+)>$(?x)/ (?x) for comments at the end

EMAIL_PATTERN = ~/^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$/

^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})

EMAIL_PATTERN = ~/[a-z[._-][\\d]]*[@][a-z[.][\\d]]*[.][a-z[.][\\d]]* /
EMAIL_PATTERN =~/.+@.+\\.[a-z]+/
EMAIL_PATTERN =~ /^[A-Z0-9+_.-]+@[A-Z0-9.-]+$/
['mkyong@yahoo.com', 'mkyong-100@yahoo.com', 'mkyong.100@yahoo.com',
'mkyong111@mkyong.com', 'mkyong-100@mkyong.net', 'mkyong.100@mkyong.com.au',
'mkyong@1.com', 'mkyong@gmail.com.com', 'mkyong+100@gmail.com', 'mkyong-100@yahoo-test.com'].each {
    println "looking at ${it}"
    print it ==~ EMAIL_PATTERN
    println "; done with ${it}"
} 
compile 'commons-validator:commons-validator:1.4.1'
        */
        /*
        def domain = theMessage.replaceFirst( 'EHLO |HELO ', '' )
        log.info "Here is the domain: ${domain}"
        if ( domain.length() > 255 ) {
            resultMap.resultString = "501 Domain name length beyond 255 char limit per RFC 3696"
            resultMap.prevCommandList = prevCommandList
        } else if ( ( domain.length() <= 255 ) && ( theMessage.firstFour() == 'EHLO' ) ) {
            prevCommandList.clear()
            prevCommandList << 'EHLO'
            resultMap.resultString = "250-Hello ${domain}\n250 HELP"
		} else if ( ( domain.length() <= 255 ) && ( theMessage.firstFour() == 'HELO' ) ) {
		    prevCommandList.clear()
		    prevCommandList << 'HELO'
		    resultMap.resultString = "250 Hello ${domain}"
		}
		resultMap.prevCommandList = prevCommandList
		resultMap.bufferMap = bufferMap
		*/
		log.info "here is resultMap: ${resultMap.toString()}"
		resultMap
    }
}

