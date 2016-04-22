package info.shelfunit.mail.meta

import groovy.sql.Sql

import groovy.util.logging.Slf4j 

class StringMetaProgrammer {
    
    static runStringMetaProgramming() {
        
        String.metaClass.firstFour = { ->
            return delegate.substring( 0, 4 )
        }
        // sometimes we need to pass the string, but not the WHOLE string
        String.metaClass.firstTen = { ->
            if ( delegate.length() < 10 ) {
                return delegate
            } else {
                return delegate.substring( 0, 10 )
            }
        }
        
        String.metaClass.allButFirstFour = { ->
            return delegate.substring( 4, delegate.length() )
        }
        String.metaClass.startsWithEHLO = { ->
            return delegate.startsWith( 'EHLO' )
        }
        String.metaClass.startsWithHELO = { ->
            return delegate.startsWith( 'HELO' ) 
        }
        String.metaClass.isHelloCommand = { ->
            ( delegate.startsWithEHLO() || delegate.startsWithHELO() )
        }
        String.metaClass.getDomain = { ->
            if ( delegate.startsWithEHLO() || delegate.startsWithHELO() ) {
                return delegate.replaceFirst( 'EHLO |HELO ', '' )
            } else {
                return null
            }
        }

        String.metaClass.static.getCommandList = { -> 
            return [ 'AUTH', 'DATA',  'MAIL', // smtp
            'DELE', 'LIST', 'RCPT', 'PASS', 'RETR', 'STAT', 'USER', // pop3  
            'RSET', 'QUIT' // both  
            ]
        }
       
        // I REALLY need to come up with a better name than this
        String.metaClass.isEncapsulated = { -> 
            def returnValue = false
            
            if ( delegate.isHelloCommand() ) { 
                returnValue = true
            } 
            String.commandList.each { comm ->
                if ( delegate.startsWith( comm ) ) {
                    returnValue = true
                }
            } // this could probably be done with "find"
            returnValue
        }  // line 200
        
        // for domain in EHLOCommand
        String.metaClass.isMoreThan255Char = { ->
            delegate.length() > 255
        }
        String.metaClass.is255CharOrLess = { ->
            delegate.length() <= 255
        }
        String.metaClass.isObsoleteCommand = { ->
            if ( delegate.length() >= 4 ) {
                return delegate.firstFour().matches( "SAML|SEND|SOML|TURN" )
            } else {
                false
            }
        }
        // at some point these might be implemented
        String.metaClass.isRFC5034Command = { ->
            if ( delegate.length() >= 4 ) {
                return delegate.firstFour().matches( "CAPA|AUTH" )
            } else {
                false
            }
        }
        // at some point these might be implemented
        String.metaClass.isOptionalPostOfficeCommand = { ->
            if ( delegate.length() >= 4 ) {
                return delegate.firstFour().matches( "TOP|UIDL|APOP" )
            } else {
                false
            }
        }
        
        String.metaClass.toInt = { ->
            def toIntResult
            try {
                toIntResult = Integer.parseInt( delegate )
            } catch ( java.lang.NumberFormatException nfe ) {
                result = 0
            }
            toIntResult
        }
        
        String.metaClass.getIntInPOP3Command { ->
            def toIntResult
            try {
                toIntResult = Integer.parseInt( delegate.allButFirstFour().trim() )
            } catch ( java.lang.NumberFormatException nfe ) {
                result = 0
            }
            toIntResult
        }
    }
}


