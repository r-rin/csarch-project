package com.github.rrin.implementation;

import com.github.rrin.util.data.DataPacket;
import com.github.rrin.interfaces.IDecrypter;
import com.github.rrin.util.data.RawData;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Decrypter implements IDecrypter, Runnable {
    private final BlockingQueue<RawData> inputQueue;
    private final BlockingQueue<DataPacket<Object>> outputQueue;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread decrypterThread;

    public Decrypter(BlockingQueue<RawData> inputQueue, BlockingQueue<DataPacket<Object>> outputQueue) {
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
                RawData rawMessage = inputQueue.take();
                DataPacket<Object> packet = DataPacket.fromByteArray(rawMessage.data, Object.class, rawMessage.connectionId);
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
