package com.github.rrin.implementation.tcp;

import com.github.rrin.interfaces.IConnectionManager;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class TcpSocketManager implements IConnectionManager<Socket> {

    private final ConcurrentHashMap<Long, Socket> activeConnections = new ConcurrentHashMap<>();
    private final AtomicLong index = new AtomicLong(0);

    public Long register(Socket socket) {
        Long id = index.incrementAndGet();
        activeConnections.put(id, socket);
        return id;
    }

    public Socket get(Long id) {
        return activeConnections.get(id);
    }

    public void remove(Long id) {
        Socket socket = activeConnections.remove(id);
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Failed to close socket: " + e.getMessage());
            }
        }
    }

    public void closeAll() {
        for (Socket socket : activeConnections.values()) {
            try {
                if (!socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                System.err.println("Failed to close socket: " + e.getMessage());
            }
        }
        activeConnections.clear();
    }

    public boolean isConnected(Long id) {
        Socket socket = activeConnections.get(id);
        return socket != null && socket.isConnected();
    }
}
