package com.github.rrin;

import com.github.rrin.implementation.Decrypter;
import com.github.rrin.implementation.Encrypter;
import com.github.rrin.implementation.Processor;
import com.github.rrin.implementation.tcp.TcpReceiver;
import com.github.rrin.implementation.tcp.TcpSender;
import com.github.rrin.implementation.tcp.TcpSocketManager;
import com.github.rrin.implementation.udp.UdpReceiver;
import com.github.rrin.implementation.udp.UdpSender;
import com.github.rrin.implementation.udp.UdpSocketManager;
import com.github.rrin.interfaces.*;
import com.github.rrin.util.data.DataPacket;
import com.github.rrin.util.data.RawData;
import com.github.rrin.util.data.RequestData;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class StoreServerUDP {
    private final BlockingQueue<RawData> rawPacketsQueue;
    private final BlockingQueue<DataPacket<Object>> parsedPacketsQueue;
    private final BlockingQueue<RequestData> responseQueue;
    private final BlockingQueue<RawData> encryptedResponseQueue;
    private final IConnectionManager<DatagramPacket> socketManager = new UdpSocketManager();

    IReceiver receiver;
    IDecrypter decrypter;
    IProcessor processor;
    IEncrypter encrypter;
    ISender sender;

    DatagramSocket socket;

    public StoreServerUDP(int receiverPort) {

        try {
            socket = new DatagramSocket(receiverPort);
        } catch (SocketException e) {
            System.out.println("Could not open UDP socket in StoreServerUDP");
            throw new RuntimeException(e);
        }

        this.rawPacketsQueue = new ArrayBlockingQueue<>(1024);
        this.parsedPacketsQueue = new ArrayBlockingQueue<>(1024);
        this.responseQueue = new ArrayBlockingQueue<>(1024);
        this.encryptedResponseQueue = new ArrayBlockingQueue<>(1024);

        this.receiver = new UdpReceiver(rawPacketsQueue, socketManager, socket);
        this.decrypter = new Decrypter(rawPacketsQueue, parsedPacketsQueue);
        this.processor = new Processor(parsedPacketsQueue, responseQueue);
        this.encrypter = new Encrypter(responseQueue, encryptedResponseQueue);
        this.sender = new UdpSender(encryptedResponseQueue, socketManager, socket);
    }

    public void start() {
        System.out.println("Store UDP server is starting...");

        receiver.start();
        decrypter.start();
        processor.start();
        encrypter.start();
        sender.start();

        System.out.println("Store UDP server has started successfully");
    }

    public void stop() {
        System.out.println("Stopping store UDP server...");

        receiver.stop();
        decrypter.stop();
        processor.stop();
        encrypter.stop();
        sender.stop();

        socket.close();

        System.out.println("Store UDP server has stopped successfully");
    }
}
