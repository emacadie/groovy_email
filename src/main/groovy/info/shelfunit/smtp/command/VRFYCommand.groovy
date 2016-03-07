package info.shelfunit.smtp.command

import groovy.util.logging.Slf4j 

/**
Per RFC 5321 Section 7.3 http://tools.ietf.org/html/rfc5321#section-7.3
and DJBernstein http://cr.yp.to/smtp/vrfy.html, 
this server will return 252. Don't give anything away
But section 3.5.2 implies I have to return an address with a 252, so I will return the argument

Per 4.1.1.6., this command has no effect on the reverse-path buffer, the forward- path buffer, or the mail data buffer.
I made it a separate class because 1. I thought I might need to before I realized it's not necessary, and 2. I might expand it in the future
*/

@Slf4j
class VRFYCommand {

    VRFYCommand(  ) { }
    
    def process( theMessage, prevCommandSet, bufferMap ) {
        def resultString
        def resultMap = [:]
        resultMap.clear()
        resultMap.bufferMap = bufferMap
        resultMap.prevCommandSet = prevCommandSet
        resultMap.resultString = "252 VRFY Disabled, returning argument ${theMesssage.allButFirstFour()}"
    }
}

