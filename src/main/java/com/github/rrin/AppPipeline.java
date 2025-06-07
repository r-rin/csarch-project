package com.github.rrin;

import com.github.rrin.dto.CommandResponse;
import com.github.rrin.implementation.Decrypter;
import com.github.rrin.implementation.Encrypter;
import com.github.rrin.implementation.Processor;
import com.github.rrin.implementation.UdpReceiver;
import com.github.rrin.interfaces.IDecrypter;
import com.github.rrin.interfaces.IEncrypter;
import com.github.rrin.interfaces.IProcessor;
import com.github.rrin.interfaces.IReceiver;
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

    public AppPipeline(int receiverPort) {
        this.rawPacketsQueue = new ArrayBlockingQueue<>(1024);
        this.parsedPacketsQueue = new ArrayBlockingQueue<>(1024);
        this.responseQueue = new ArrayBlockingQueue<>(1024);
        this.encryptedResponseQueue = new ArrayBlockingQueue<>(1024);

        this.receiver = new UdpReceiver(rawPacketsQueue, receiverPort);
        this.decrypter = new Decrypter(rawPacketsQueue, parsedPacketsQueue);
        this.processor = new Processor(parsedPacketsQueue, responseQueue);
        this.encrypter = new Encrypter(responseQueue, encryptedResponseQueue);

    }

    public void start() {
        System.out.println("App pipeline is starting...");

        receiver.start();
        decrypter.start();
        processor.start();

        System.out.println("Pipeline has started successfully");
    }

    public void stop() {
        System.out.println("Stopping app pipeline...");

        receiver.stop();
        decrypter.stop();
        processor.stop();

        System.out.println("Pipeline has stopped successfully");
    }
}
