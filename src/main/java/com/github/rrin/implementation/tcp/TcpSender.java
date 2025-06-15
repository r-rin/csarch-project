package com.github.rrin.implementation.tcp;

import com.github.rrin.interfaces.IConnectionManager;
import com.github.rrin.interfaces.ISender;
import com.github.rrin.util.Converter;
import com.github.rrin.util.data.RawData;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class TcpSender implements ISender, Runnable {

    private final BlockingQueue<RawData> inputQueue;
    private final IConnectionManager<Socket> socketManager;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread senderThread;

    public TcpSender(BlockingQueue<RawData> inputQueue, IConnectionManager<Socket> socketManager) {
        this.inputQueue = inputQueue;
        this.socketManager = socketManager;
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            senderThread = new Thread(this, "TcpSender");
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
        Socket socket = socketManager.get(responseData.connectionId);

        if (socket == null || socket.isClosed()) {
            System.err.println("Connection " + responseData.connectionId + " is no longer available");
            return;
        }

        try {
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(responseData.data);
            outputStream.flush();
            System.out.println("Sent response to: " + socket.getInetAddress() + ":" + socket.getPort() +" with data: " + Converter.bytesToHex(responseData.data));
        } catch (IOException e) {
            System.err.println("Failed to send response to connection " + responseData.connectionId + ": " + e.getMessage());
            socketManager.remove(responseData.connectionId);
        }
    }
}
