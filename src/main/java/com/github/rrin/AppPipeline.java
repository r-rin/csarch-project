package com.github.rrin;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class AppPipeline {
    private final BlockingQueue<byte[]> rawPacketsQueue;
    private final BlockingQueue<DataPacket<?>> parsedPacketsQueue;
    private final BlockingQueue<DataPacket<?>> responseQueue;
    private final BlockingQueue<byte[]> encryptedResponseQueue;

    public AppPipeline() {
        this.rawPacketsQueue = new ArrayBlockingQueue<>(1024);
        this.parsedPacketsQueue = new ArrayBlockingQueue<>(1024);
        this.responseQueue = new ArrayBlockingQueue<>(1024);
        this.encryptedResponseQueue = new ArrayBlockingQueue<>(1024);
    }

    public void start() {
        // start threads
    }

    public void stop() {
        // stop thread and exit
    }
}
