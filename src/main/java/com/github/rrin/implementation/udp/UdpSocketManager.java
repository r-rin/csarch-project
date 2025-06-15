package com.github.rrin.implementation.udp;

import com.github.rrin.interfaces.IConnectionManager;

import java.net.DatagramPacket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class UdpSocketManager implements IConnectionManager<DatagramPacket> {

    private final ConcurrentHashMap<Long, DatagramPacket> activeConnections = new ConcurrentHashMap<>();
    private final AtomicLong index = new AtomicLong(0);

    public Long register(DatagramPacket packet) {
        Long id = index.incrementAndGet();
        activeConnections.put(id, packet);
        return id;
    }

    public DatagramPacket get(Long id) {
        return activeConnections.get(id);
    }

    public void remove(Long id) {
        activeConnections.remove(id);
    }

    public void closeAll() {
        activeConnections.clear();
    }

    public boolean isConnected(Long id) {
        return false;
    }
}
