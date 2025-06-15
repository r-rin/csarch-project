package com.github.rrin.implementation.udp;

import com.github.rrin.interfaces.IConnectionManager;
import com.github.rrin.interfaces.IReceiver;
import com.github.rrin.util.Converter;
import com.github.rrin.util.data.RawData;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class UdpReceiver implements IReceiver, Runnable {

    private final BlockingQueue<RawData> outputQueue;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final IConnectionManager<DatagramPacket> packetManager;

    private final DatagramSocket socket;
    private Thread receiverThread;
    private ExecutorService clientHandlersPool;

    public UdpReceiver(BlockingQueue<RawData> outputQueue, IConnectionManager<DatagramPacket> packetManager, DatagramSocket socket) {
        this.outputQueue = outputQueue;
        this.socket = socket;
        this.packetManager = packetManager;
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            clientHandlersPool = Executors.newCachedThreadPool();
            receiverThread = new Thread(this, "UdpReceiver");
            receiverThread.start();
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

            clientHandlersPool.shutdownNow();
            packetManager.closeAll();

            try {
                if (receiverThread != null) {
                    receiverThread.join();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
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

                long id = packetManager.register(packet);

                // Extract the actual data from the packet
                byte[] receivedData = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), packet.getOffset(), receivedData, 0, packet.getLength());

                System.out.println("Received packet from " + packet.getAddress() + ":" + packet.getPort() + " (" + receivedData.length + " bytes)");
                System.out.println("Data received: " + Converter.bytesToHex(receivedData));
                outputQueue.put(new RawData(receivedData, id));

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
