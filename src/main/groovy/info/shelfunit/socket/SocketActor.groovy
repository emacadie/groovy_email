package info.shelfunit.socket

import java.net.ServerSocket
import groovyx.gpars.actor.DefaultActor
import groovyx.gpars.actor.DynamicDispatchActor
import groovyx.gpars.actor.StaticDispatchActor

class SocketActor extends DynamicDispatchActor {
    
    void onMessage( SocketHolder sHold ) {
        def ServerSocket = sHold.getSocket()
    }
    
    void onMessage( Object message ) { 
        println( "Handling default message" )
    }
}

