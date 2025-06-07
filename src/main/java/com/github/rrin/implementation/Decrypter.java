package com.github.rrin.implementation;

import com.github.rrin.DataPacket;
import com.github.rrin.interfaces.IDecrypter;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Decrypter implements IDecrypter, Runnable {
    private final BlockingQueue<byte[]> inputQueue;
    private final BlockingQueue<DataPacket<?>> outputQueue;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread decrypterThread;

    public Decrypter(BlockingQueue<byte[]> inputQueue, BlockingQueue<DataPacket<?>> outputQueue) {
        this.inputQueue = inputQueue;
        this.outputQueue = outputQueue;
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            decrypterThread = new Thread(this, "Decrypter");
            decrypterThread.start();
            System.out.println("Decrypter started");
        }
    }

    @Override
    public void stop() {
        running.set(false);
        if (decrypterThread != null) {
            decrypterThread.interrupt();
        }
        System.out.println("Decrypter stopped");
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public void run() {
        while (running.get()) {
            try {
                byte[] rawMessage = inputQueue.take();
                DataPacket<Object> packet = DataPacket.fromByteArray(rawMessage, Object.class);
                outputQueue.put(packet);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Error parsing message: " + e.getMessage());
            }
        }
    }
}
