package info.shelfunit.socket.command


import groovy.util.logging.Slf4j 
// reg ex for email from http://www.mkyong.com/regular-expressions/how-to-validate-email-address-with-regular-expression/
// look at http://www.ngdc.noaa.gov/wiki/index.php?title=Regular_Expressions_in_Groovy as well
// RFC 5321 4.1.1 The reverse-path is the argument of the MAIL command, the forward-path is the argument of
//   the RCPT command, and the mail data is the argument of the DATA command. 

@Slf4j
class MAILCommand {
    
    static rg =  '''(?ix)          # enable case-insensitive matches, extended patterns
            (\\d+)         # 1: The disk space we want
            \\s+           # some whitespace
            \\d+%          # a number followed by %
            \\s+           # some more whitespace
            (/nfs/data.*)  # 2: partition name'''
           
    static regex = '''~/^(MAIL FROM:)<
     [_A-Za-z0-9-\\+]+	# must start with string in the bracket [ ], must contains one or more - the plus sign
     (			        # start of group 1
     \\.[_A-Za-z0-9-]+	# follow by a dot  and string in the bracket [ ], must contains one or more 
     )*			        # end of group 1, this group is optional = the star
     @			        # must contains a "@" symbol
     [A-Za-z0-9-]+      # follow by string in the bracket [ ], must contains one or more 
     (			        # start of group 2 - first level TLD checking
     \\.[A-Za-z0-9]+    # follow by a dot "." and string in the bracket [ ], must contains one or more 
     )*		            # end of group 2, this group is optional 
     (			        # start of group 3 - second level TLD checking
     \\.[A-Za-z]{2,}    # follow by a dot  and string in the bracket [ ], with minimum length of 2
     )			        # end of group 3
    >$/'''
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
~/^(MAIL FROM:)<[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})>$/
q = mm =~ mP // get the matcher as q
to view the matches
q[0][1]
I have no idea why you need a 2-dimensional array

        */
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
		log.info "here is resultMap: ${resultMap.toString()}"
		resultMap
    }
}

