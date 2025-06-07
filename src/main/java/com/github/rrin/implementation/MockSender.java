package com.github.rrin.implementation;

import com.github.rrin.DataPacket;
import com.github.rrin.interfaces.ISender;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.rrin.util.Converter.bytesToHex;

public class MockSender implements ISender, Runnable {
    private final BlockingQueue<byte[]> inputQueue;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread senderThread;

    public MockSender(BlockingQueue<byte[]> inputQueue) {
        this.inputQueue = inputQueue;
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            senderThread = new Thread(this, "MockSender");
            senderThread.start();
            System.out.println("MockSender started");
        }
    }

    @Override
    public void stop() {
        running.set(false);
        if (senderThread != null) {
            senderThread.interrupt();
        }
        System.out.println("MockSender stopped");
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public void run() {
        while (running.get()) {
            try {
                byte[] responseData = inputQueue.take();
                displayResponse(responseData);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Error sending response: " + e.getMessage());
            }
        }
    }

    private void displayResponse(byte[] responseData) {
        try {
            System.out.println("===== SENDING RESPONSE =====");
            System.out.println("Response size: " + responseData.length + " bytes");
            System.out.println("Response hex: " + bytesToHex(responseData));

            DataPacket<Object> packet = DataPacket.fromByteArray(responseData, Object.class);
            System.out.println("Response packet ID: " + packet.getPacketId());
            System.out.println("Response from source: " + packet.getSourceId());
            System.out.println("Response user ID: " + packet.getBody().getUserId());
            System.out.println("Response data: " + packet.getBody().getData());
            System.out.println("============================");
        } catch (Exception e) {
            System.err.println("Could not parse response for display: " + e.getMessage());
        }
    }
}
