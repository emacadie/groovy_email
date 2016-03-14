package info.shelfunit.mail.meta

import groovy.util.logging.Slf4j 

@Slf4j
class MatcherMetaProgrammer {
    
    static runMatcherMetaProgramming() {
        java.util.regex.Matcher.metaClass.extractUserNameInRCPT = { ->
            delegate[ 0 ][ 2 ].substring( 0, ( delegate[ 0 ][ 2 ].length() - ( delegate[ 0 ][ 3 ].length() + 1 ) ) )
        }
        java.util.regex.Matcher.metaClass.getEmailAddressInRCPT = { ->
            delegate[ 0 ][ 2 ]
        }
        java.util.regex.Matcher.metaClass.extractDomainRCPT = { ->
            delegate[ 0 ][ 3 ]
        }
        java.util.regex.Matcher.metaClass.getEmailAddressInMAIL = { ->
            delegate[ 0 ][ 2 ]
        }
        java.util.regex.Matcher.metaClass.getDomainInMAIL = { ->
            delegate[ 0 ][ 3 ]
        }
        java.util.regex.Matcher.metaClass.handles8BitInMAIL = { ->
            delegate[ 0 ][ 4 ]
        }
        java.util.regex.Matcher.metaClass.getWholeFromAddressInMSSG = { ->
            delegate[ 0 ][ 1 ]
        }
        java.util.regex.Matcher.metaClass.getUserNameInMSSG = { ->
            delegate[ 0 ][ 2 ]
        }
        java.util.regex.Matcher.metaClass.getFromDomainInMSSG = { ->
            delegate[ 0 ][ 3 ]
        }
        java.util.regex.Matcher.metaClass.getPasswordInPASS = { ->
            delegate[ 0 ][ 2 ]
        }
        java.util.regex.Matcher.metaClass.getBase64InAuth = { ->
            delegate[ 0 ][ 2 ]
        }
        
    }
}

