package info.shelfunit.socket.command

import org.xbill.DNS.Address

import groovy.util.logging.Slf4j 

@Slf4j
class EHLOCommand {
    def resultMap = [:]
    
    // RFC 5321 Sec.4.1.4: If the EHLO command is not acceptable to the SMTP server, 501, 500,
    //    502, or 550 failure replies MUST be returned as appropriate. 
    // https://tools.ietf.org/html/rfc3696#section-3: Domain cannot be more than 255 chars
    // RFC 5321, 3.2.  Client Initiation: You must return simple HELO 
    def process( theMessage, prevCommandList ) {
        def resultString
        resultMap.clear()
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
		log.info "here is resultMap: ${resultMap.toString()}"
		resultMap
    }
    
    // RFC 5321, Section 4.1.4.: Order of Commands and Section 7.9.: Scope of Operation of SMTP Servers: 
    // Do not reject a message due to a bad address. The internet might stop working.
    
    
    def processDomain( domain ) {
        def addressGood = true
        def hostAddress
        try {
            // Some networks return '198.105.254.228' for IPAddress for invalid domains
            InetAddress addr = Address.getByName( domain )
            hostAddress = addr.hostAddress
        } catch ( java.net.UnknownHostException uhEx ) {
            println "Got UnknownHostException in processDomain"
            uhEx.printStackTrace()
            addressGood = false
            hostAddress = 'X.X.X.X'
        }
        println "is addressGood? ${addressGood}"
        hostAddress
    }
}

