import java.net.ServerSocket
def server = new ServerSocket( 4444 )
 
while ( true ) {
    server.accept { socket ->
        println "processing new connection..."
        socket.withStreams { input, output ->
            
            // buffer = reader.readLine()
            println "About to try reading buffer"
            // def buffer = input.newReader().readLine() // okay
            // def buffer = input.newReader().readLines() // no good
            // def buffer = input.newReader().getText() // no good
            def sBuffer = new StringBuffer()
            def holdLine
            def reader = input.newReader()
            def theLine = reader.readLine()
            println "First line: ${theLine}"
            while ( !theLine.startsWith( "XX" ) ) {
                println "Here is theLine before read: ${theLine}  and it's a ${theLine.getClass().getName()}"
                try {
                theLine = reader?.readLine()
                } catch ( Exception ex ) {
                    println "exception: ${ex.printMessage()}"
                    ex.printStackTrace()
                }
                println "Here is theLine after read: ${theLine}"
                if ( theLine.endsWith( "\r\n" ) ) { println "line ends with CRLF" 
                } else if ( theLine.endsWith( "\n" ) )  { println "line ends with LF" 
                } else if ( theLine.endsWith( "\r" ) )  { println "line ends with CR" 
                } else { println "line ends with something else" }
                if ( theLine.matches( ".*\n" ) ) { println "regex says line ends with LF" }
                
            }
            println "Here is theLine: ${theLine}"
            if ( theLine.endsWith( "\r\n" ) ) { println "line ends with CRLF" 
            } else if ( theLine.endsWith( "\r" ) )  { println "line ends with CR" }
            println "Done iterating"

            def buffer = "hello" // sBuffer.toString()
            println "server received: ${buffer}"
            now = new Date()
            output << "echo-response($now): " + buffer + "\n"
        }
        println "processing/thread complete------------------------"
    }
}

///////////////////////////
Car.metaClass.invokeMethod = { String name, args ->

    System.out.print("Call to $name intercepted... ")
    
    if (name != 'check') {
        System.out.print("running filter... ")
        Car.metaClass.getMetaMethod('check').invoke(delegate, null)
    }
    def validMethod = Car.metaClass.getMetaMethod(name, args)
    if (validMethod != null) {
        validMethod.invoke(delegate, args)
    } else {
        Car.metaClass.invokeMissingMethod(delegate, name, args)
    }
}
ExpandoMetaClass.enableGlobally()
java.util.ArrayList.metaClass.invokeMethod = { String name, args ->

    System.out.print("Call to $name intercepted... ")
    
    if ( name == 'plus' ) {
        System.out.print( "running filter on plus... ")
        java.util.ArrayList.metaClass.getMetaMethod( 'plus' ).invoke( delegate, args )
    }
    List.metaClass.add = { Object[] varArgs ->
        delegateTo.invokeMethod('add', varArgs)
    }
    /*
    def validMethod = Car.metaClass.getMetaMethod(name, args)
    if (validMethod != null) {
        validMethod.invoke(delegate, args)
    } else {
        Car.metaClass.invokeMissingMethod(delegate, name, args)
    }
    */
}

def intMethod = Test.metaClass.getMetaMethod("foo", [Integer] as Class[] )
def strMethod = Test.metaClass.getMetaMethod("foo", [String] as Class[] )

def intMethod = List.metaClass.getMetaMethod( "add", [Integer, Object] as Object[] )
def objMethod = List.metaClass.getMetaMethod( "add", [Object] as Object[] )

ExpandoMetaClass.enableGlobally()

intMethod = java.util.ArrayList.metaClass.getMetaMethod( "add", [ new Integer( 5 ), new Object() ] as Object[] )
objMethod = java.util.ArrayList.metaClass.getMetaMethod( "add", [ Object ] as Object[] )

java.util.ArrayList.metaClass.add = { int arg, Object argO ->
    println "foo(int) intercepted"
    intMethod.invoke(delegate, [ arg, argO ] )
}
java.util.ArrayList.metaClass.add = { arg ->
    println "foo(String) intercepted"
    objMethod.invoke( delegate, arg )
}
java.util.ArrayList.metaClass.add = { args ->
    println "add intercepted"
    // objMethod.invoke( delegate, arg )
}
aa = []
aa.add(  1  )
aa.add( 'p' )
aa.add( 2, new Date() )
/////////////////////
intSBMethod = StringBuffer.metaClass.getMetaMethod( "append", [ Integer.parseInt( "100" ) ] as Object[] )
stringSBMethod = StringBuffer.metaClass.getMetaMethod( "append", [ String.valueOf( 100 ) ] as Object[] )

StringBuffer.metaClass.append = { int arg ->
    println "foo(int) intercepted"
    intSBMethod.invoke( delegate, arg  )
}
StringBuffer.metaClass.append = { String arg ->
    println "foo(String) intercepted"
    stringSBMethod.invoke( delegate, arg )
}

sb = new StringBuffer()
sb.append( 3 )
sb.append( " hello" )
println "sb: ${sb}"
/////////////////////////
java.util.ArrayList.metaClass.constructor = {  ->
println "Intercepting constructor call"
constructor = ArrayList.class.getConstructor( null )
constructor.newInstance(  ).asImmutable()
}
// qq = ArrayList.class.getConstructor(null)
// qq.newInstance()
// qq.newInstance().asImmutable()

java.util.ArrayList.metaClass.constructor = { int arg ->
    println "Intercepting int constructor call"
    constructor = ArrayList.class.getConstructor( int )
    constructor.newInstance( arg ).asImmutable()
}
java.util.ArrayList.metaClass.constructor = { java.util.Collection arg ->
    println "Intercepting Collection call"
    constructor = ArrayList.class.getConstructor( java.util.Collection )
    println "just made constructor: ${constructor}"
    constructor.newInstance( arg ).asImmutable()
}

java.util.ArrayList.metaClass.constructor = {  ->
    println "Intercepting constructor call"
    constructor = ArrayList.class.getConstructor( null )
    constructor.newInstance(  ).asImmutable()
}

rr = []

rr.class.name

tt = ArrayList.class.getConstructor(int)

uu = tt.newInstance(9)

uu.class.name

java.util.ArrayList.metaClass.constructor = { int arg ->
println "Intercepting int constructor call"
constructor = ArrayList.class.getConstructor( int )
constructor.newInstance( arg ).asImmutable()
}

// java.util.Collections$UnmodifiableRandomAccessList.plus = { Object arg ->
//     delegate.plus( arg ).asImmutable()
// }

