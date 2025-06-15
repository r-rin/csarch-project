package com.github.rrin;

import com.github.rrin.implementation.*;
import com.github.rrin.implementation.tcp.TcpReceiver;
import com.github.rrin.implementation.tcp.TcpSender;
import com.github.rrin.implementation.tcp.TcpSocketManager;
import com.github.rrin.interfaces.*;
import com.github.rrin.util.data.*;

import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class StoreServerTCP {
    private final BlockingQueue<RawData> rawPacketsQueue;
    private final BlockingQueue<DataPacket<Object>> parsedPacketsQueue;
    private final BlockingQueue<RequestData> responseQueue;
    private final BlockingQueue<RawData> encryptedResponseQueue;
    private final IConnectionManager<Socket> socketManager = new TcpSocketManager();

    IReceiver receiver;
    IDecrypter decrypter;
    IProcessor processor;
    IEncrypter encrypter;
    ISender sender;

    public StoreServerTCP(int receiverPort) {
        this.rawPacketsQueue = new ArrayBlockingQueue<>(1024);
        this.parsedPacketsQueue = new ArrayBlockingQueue<>(1024);
        this.responseQueue = new ArrayBlockingQueue<>(1024);
        this.encryptedResponseQueue = new ArrayBlockingQueue<>(1024);

        this.receiver = new TcpReceiver(rawPacketsQueue, receiverPort, socketManager);
        this.decrypter = new Decrypter(rawPacketsQueue, parsedPacketsQueue);
        this.processor = new Processor(parsedPacketsQueue, responseQueue);
        this.encrypter = new Encrypter(responseQueue, encryptedResponseQueue);
        this.sender = new TcpSender(encryptedResponseQueue, socketManager);
    }

    public void start() {
        System.out.println("Store TCP server is starting...");

        receiver.start();
        decrypter.start();
        processor.start();
        encrypter.start();
        sender.start();

        System.out.println("Store TCP server has started successfully");
    }

    public void stop() {
        System.out.println("Stopping store TCP server...");

        receiver.stop();
        decrypter.stop();
        processor.stop();
        encrypter.stop();
        sender.stop();

        System.out.println("Store TCP server has stopped successfully");
    }
}
