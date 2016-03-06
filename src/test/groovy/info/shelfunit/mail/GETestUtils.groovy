package info.shelfunit.mail

class GETestUtils {
    static def getBase64Hash( fname, password ) {
        "${Character.MIN_VALUE}${fname}${Character.MIN_VALUE}${password}".bytes.encodeBase64().toString()
    }
    
    static def alphabet =  ( 'a'..'z' ).join()
    
    static def getRandomString = { int n = 9 ->
        
        new Random().with {
            ( 1..n ).collect { 
                alphabet[ nextInt( alphabet.length() ) ] 
            }.join()
        }
    }
}

