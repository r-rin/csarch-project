package com.github.rrin;

import com.github.rrin.implementation.*;
import com.github.rrin.interfaces.*;
import com.github.rrin.util.RequestData;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class AppPipeline {
    private final BlockingQueue<byte[]> rawPacketsQueue;
    private final BlockingQueue<DataPacket<?>> parsedPacketsQueue;
    private final BlockingQueue<RequestData> responseQueue;
    private final BlockingQueue<byte[]> encryptedResponseQueue;

    // Pipeline components
    IReceiver receiver;
    IDecrypter decrypter;
    IProcessor processor;
    IEncrypter encrypter;
    ISender sender;

    public AppPipeline(int receiverPort) {
        this.rawPacketsQueue = new ArrayBlockingQueue<>(1024);
        this.parsedPacketsQueue = new ArrayBlockingQueue<>(1024);
        this.responseQueue = new ArrayBlockingQueue<>(1024);
        this.encryptedResponseQueue = new ArrayBlockingQueue<>(1024);

        this.receiver = new UdpReceiver(rawPacketsQueue, receiverPort);
        this.decrypter = new Decrypter(rawPacketsQueue, parsedPacketsQueue);
        this.processor = new Processor(parsedPacketsQueue, responseQueue);
        this.encrypter = new Encrypter(responseQueue, encryptedResponseQueue);
        this.sender = new MockSender(encryptedResponseQueue);

    }

    public void start() {
        System.out.println("App pipeline is starting...");

        receiver.start();
        decrypter.start();
        processor.start();
        encrypter.start();
        sender.start();

        System.out.println("Pipeline has started successfully");
    }

    public void stop() {
        System.out.println("Stopping app pipeline...");

        receiver.stop();
        decrypter.stop();
        processor.stop();
        encrypter.stop();
        sender.stop();

        System.out.println("Pipeline has stopped successfully");
    }
}
