package info.shelfunit.mail

class GETestUtils {
    static def getBase64Hash( fname, password ) {
        "${Character.MIN_VALUE}${fname}${Character.MIN_VALUE}${password}".bytes.encodeBase64().toString()
    }
}

