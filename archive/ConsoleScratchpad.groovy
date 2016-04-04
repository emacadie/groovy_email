/*
------------------------+---------------------------------+---------------
 malcolm@shelfunit.info | eeee@wilson.net                 | ENTERED
 malcolm@shelfunit.info | eeee@gmail.com                  | ENTERED
 malcolm@shelfunit.info | eeee@gmail.com,eeee@yahoo.com   | ENTERED
 malcolm@shelfunit.info | eeee@gmail.com                  | ENTERED
 malcolm@shelfunit.info | eeee@yahoo.com                  | ENTERED
 malcolm@ShelfUnit.info | dan@chumbawumba                 | ENTERED
 malcolm@shelfunit.info | eeee@wilson.net                 | ENTERED
 malcolm@ShelfUnit.info | dan@chumbawumba                 | ENTERED
*/
// good one
// '''([\\w!#$%&’*+/=?`{|}~^-]+(?:\\.[\\w!#$%&’*+/=?`{|}~^-]+)*@((?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}))$(?x)'''
// '''([\\w!#$%&’*+/=?`{|}~^-]+(?:\\.[\\w!#$%&’*+/=?`{|}~^-]+)*@((?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}))$(?x)'''
/*
here is q[ 0 ][ 0 ]: eeee@yahoo.com
here is q[ 0 ][ 1 ]: eeee@yahoo.com
here is q[ 0 ][ 2 ]: eeee
here is q[ 0 ][ 3 ]: yahoo.com
*/
import info.shelfunit.mail.meta.MetaProgrammer

MetaProgrammer.runMetaProgramming()
def localPart  = '''(([\\w!#$%&’*+/=?`{|}~^-]+(?:\\.[\\w!#$%&’*+/=?`{|}~^-]+)*)@'''
def domainName = '''((?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}))'''
def regex = localPart + domainName  
def domainList = [ 'shelfunit.info', 'groovy-is-good.com' ]
def toListString = 'eeeeG@gmail.com,eeeeY@yahoo.com,ddddG@gmail.com,ddddY@yahoo.com,this.is@cnn.com' 
def toAddressList = toListString.split( ',' )
def outgoingMap = [:]
toAddressList.each { address ->
    println "Here is addr: ${addr}"
    def q = address =~ regex
    println "here is q[ 0 ][ 0 ]: ${q[ 0 ][ 0 ]}"
    println "here is q[ 0 ][ 1 ]: ${q[ 0 ][ 1 ]}"
    println "here is q[ 0 ][ 2 ]: ${q[ 0 ][ 2 ]}"
    println "here is q[ 0 ][ 3 ]: ${q[ 0 ][ 3 ]}"
    q.each { match ->
        match.eachWithIndex { group, n ->
            println "${n}, <$group>"
        }
    }
    outgoingMap.addDomainToOutboundMap( q.getDomainInOutboundSpool() )
    outgoingMap[ q.getDomainInOutboundSpool() ] << q.getUserInOutboundSpool()
    println "------"
}
outgoingMap.each { k, v ->
    println "++++"
    println "Key ${k} has value ${v}"
}


