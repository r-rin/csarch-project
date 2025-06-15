package com.github.rrin.implementation.tcp;

import com.github.rrin.interfaces.IConnectionManager;
import com.github.rrin.interfaces.IReceiver;
import com.github.rrin.util.data.RawData;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class TcpReceiver implements IReceiver, Runnable {

    private final BlockingQueue<RawData> outputQueue;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final int port;
    private final IConnectionManager<Socket> socketManager;

    private ServerSocket socket;
    private Thread receiverThread;
    private ExecutorService clientHandlersPool;

    public TcpReceiver(BlockingQueue<RawData> outputQueue, int port, IConnectionManager<Socket> socketManager) {
        this.outputQueue = outputQueue;
        this.port = port;
        this.socketManager = socketManager;
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            clientHandlersPool = Executors.newCachedThreadPool();
            receiverThread = new Thread(this, "TcpReceiver");
            receiverThread.start();
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            clientHandlersPool.shutdownNow();
            socketManager.closeAll();

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
        try {
            this.socket = new ServerSocket(port);

            while (running.get()) {
                Socket clientSocket = socket.accept();
                long connId = socketManager.register(clientSocket);
                clientHandlersPool.submit(() -> createClientConnection(clientSocket, connId));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            running.set(false);
        }
    }

    private void createClientConnection(Socket clientSocket, long connId) {
        try {
            while (running.get() && !clientSocket.isClosed()) {
                InputStream inputStream = clientSocket.getInputStream();
                byte[] buffer = new byte[2048];
                int bytesRead = inputStream.read(buffer);

                if (bytesRead > 0) {
                    byte[] data = new byte[bytesRead];
                    System.arraycopy(buffer, 0, data, 0, bytesRead);

                    try {
                        outputQueue.put(new RawData(data, connId));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else if (bytesRead == -1) {
                    // Client has disconnected
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error during connection: " + e.getMessage());
        } finally {
            socketManager.remove(connId);
        }
    }
}
