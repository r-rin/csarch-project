package com.github.rrin.implementation;

import com.github.rrin.Main;
import com.github.rrin.interfaces.IReceiver;
import com.github.rrin.util.Converter;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class UdpReceiver implements IReceiver, Runnable {
    private final BlockingQueue<byte[]> outputQueue;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final int port;
    private DatagramSocket socket;
    private Thread receiverThread;

    public UdpReceiver(BlockingQueue<byte[]> outputQueue, int port) {
        this.outputQueue = outputQueue;
        this.port = port;
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            try {
                socket = new DatagramSocket(port);
                //socket.setSoTimeout(1000);
                receiverThread = new Thread(this, "UDPReceiver");
                receiverThread.start();
                System.out.println("UDPReceiver started on port " + port);
            } catch (SocketException e) {
                running.set(false);
                throw new RuntimeException("Failed to start UDPReceiver", e);
            }
        }
    }

    @Override
    public void stop() {
        running.set(false);
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        if (receiverThread != null) {
            receiverThread.interrupt();
        }
        System.out.println("UDPReceiver stopped");
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public void run() {
        byte[] buffer = new byte[2048];

        while (running.get() && !socket.isClosed()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                // Extract the actual data from the packet
                byte[] receivedData = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), packet.getOffset(), receivedData, 0, packet.getLength());

                System.out.println("Received packet from " + packet.getAddress() + ":" + packet.getPort() + " (" + receivedData.length + " bytes)");
                System.out.println("Data received: " + Converter.bytesToHex(receivedData));
                outputQueue.put(receivedData);

            } catch (SocketTimeoutException ignored) {
            } catch (IOException e) {
                if (running.get()) {
                    System.err.println("Error receiving data: " + e.getMessage());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
