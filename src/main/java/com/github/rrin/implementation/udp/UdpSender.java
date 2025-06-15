package com.github.rrin.implementation.udp;

import com.github.rrin.interfaces.IConnectionManager;
import com.github.rrin.interfaces.ISender;
import com.github.rrin.util.Converter;
import com.github.rrin.util.data.RawData;

import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class UdpSender implements ISender, Runnable {

    private final BlockingQueue<RawData> inputQueue;
    private final IConnectionManager<DatagramPacket> socketManager;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread senderThread;
    private DatagramSocket socket;

    public UdpSender(BlockingQueue<RawData> inputQueue, IConnectionManager<DatagramPacket> socketManager, DatagramSocket socket) {
        this.inputQueue = inputQueue;
        this.socketManager = socketManager;
        this.socket = socket;
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            senderThread = new Thread(this, "UdpSender");
            senderThread.start();
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            if (senderThread != null) {
                senderThread.interrupt();
                try {
                    senderThread.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public void run() {
        while (running.get()) {
            try {
                RawData responseData = inputQueue.take();
                sendResponse(responseData);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void sendResponse(RawData responseData) {
        DatagramPacket clientPacket = socketManager.get(responseData.connectionId);

        if (clientPacket == null) {
            System.err.println("UDP packet with ID " + responseData.connectionId + " is no longer available");
            return;
        }

        try {
            InetAddress clientAddress = clientPacket.getAddress();
            int clientPort = clientPacket.getPort();

            DatagramPacket responsePacket = new DatagramPacket(
                    responseData.data,
                    responseData.data.length,
                    clientAddress,
                    clientPort
            );

            socket.send(responsePacket);

            System.out.println("Sent UDP response to: " + clientAddress + ":" + clientPort +
                    " with data: " + Converter.bytesToHex(responseData.data));

        } catch (IOException e) {
            System.err.println("Failed to send UDP response to connection " + responseData.connectionId + ": " + e.getMessage());
            socketManager.remove(responseData.connectionId);
        }
    }
}
