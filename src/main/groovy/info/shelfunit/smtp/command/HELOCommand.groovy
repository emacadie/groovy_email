package info.shelfunit.smtp.command

import org.xbill.DNS.Address

import groovy.util.logging.Slf4j 

@Slf4j
class HELOCommand {
    def resultMap = [:]
    
    // RFC 5321 Sec.4.1.4: If the EHLO command is not acceptable to the SMTP server, 501, 500,
    //    502, or 550 failure replies MUST be returned as appropriate. 
    // https://tools.ietf.org/html/rfc3696#section-3: Domain cannot be more than 255 chars
    def process( theMessage, prevCommandSet ) {
        def resultString
        resultMap.clear()
        def domain
        domain = theMessage.replaceFirst( 'HELO ', '' ) 
        if ( domain.length() > 255 ) {
            resultString = "501 Domain name length beyond 255 char limit per RFC 3696"
        } else {
            prevCommandSet.clear()
            resultString = "250 Hello ${domain}"
        }
        resultMap.resultString   = resultString
        resultMap.prevCommandSet = prevCommandSet
    }
    
    // RFC 5321, Section 4.1.4.: Order of Commands and Section 7.9.: Scope of Operation of SMTP Servers: 
    // Do not reject a message due to a bad address. The internet might stop working.
    
    def processDomain( domain ) {
        def addressGood = true
        def hostAddress
        try {
            // Some networks return '198.105.254.228' for IPAddress for invalid domains
            InetAddress addr = Address.getByName( domain )
            hostAddress      = addr.hostAddress
        } catch ( java.net.UnknownHostException uhEx ) {
            addressGood = false
            hostAddress = 'X.X.X.X'
        }
        hostAddress
    }
}

