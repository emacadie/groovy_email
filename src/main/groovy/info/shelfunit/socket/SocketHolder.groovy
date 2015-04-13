package info.shelfunit.socket

import java.net.ServerSocket

final class SocketHolder {
    final ServerSocket socket
    
    SocketHolder( argSocket ) {
        socket = argSocket
    }
    
}

