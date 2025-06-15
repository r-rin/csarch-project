package com.github.rrin.implementation;

import com.github.rrin.util.data.DataPacket;
import com.github.rrin.interfaces.IEncrypter;
import com.github.rrin.util.CommandType;
import com.github.rrin.util.data.RawData;
import com.github.rrin.util.data.RequestData;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Encrypter implements IEncrypter, Runnable {
    private final BlockingQueue<RequestData> inputQueue;
    private final BlockingQueue<RawData> outputQueue;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread encrypterThread;

    public Encrypter(BlockingQueue<RequestData> inputQueue, BlockingQueue<RawData> outputQueue) {
        this.inputQueue = inputQueue;
        this.outputQueue = outputQueue;
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            encrypterThread = new Thread(this, "Encrypter");
            encrypterThread.start();
            System.out.println("Encrypter started");
        }
    }

    @Override
    public void stop() {
        running.set(false);
        if (encrypterThread != null) {
            encrypterThread.interrupt();
        }
        System.out.println("Encrypter stopped");
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public void run() {
        while (running.get()) {
            try {
                RequestData response = inputQueue.take();
                byte[] encryptedResponse = encryptResponse(response);
                outputQueue.put(new RawData(encryptedResponse, response.getConnectionId()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Error encrypting response: " + e.getMessage());
            }
        }
    }

    private byte[] encryptResponse(RequestData response) {

        DataPacket<Object> packet = new DataPacket<>(
                (byte) 0x13,
                response.getSourceId(),
                response.getPacketId(),
                CommandType.RESPONSE,
                response.getUserId(),
                response.getResponse(),
                0
        );

        return packet.toByteArray();
    }
}
