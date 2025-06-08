package com.github.rrin;

import com.github.rrin.implementation.UdpReceiver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class UdpReceiverTest {

    private BlockingQueue<byte[]> outputQueue;
    private UdpReceiver udpReceiver;
    private int testPort;


    @BeforeEach
    void setUp() throws Exception {
        outputQueue = new ArrayBlockingQueue<>(10);
        testPort = 5555;
        udpReceiver = new UdpReceiver(outputQueue, testPort);
    }

    @AfterEach
    void shutDown() throws Exception {
        if (udpReceiver.isRunning()) {
            udpReceiver.stop();
        }
    }

    @Test
    void testStartUdpReceiver() {
        assertFalse(udpReceiver.isRunning());
        udpReceiver.start();
        assertTrue(udpReceiver.isRunning());
    }

    @Test
    void testMultipleThreads_shouldBeOneThread() throws Exception {
        udpReceiver.start();
        assertTrue(udpReceiver.isRunning());

        udpReceiver.stop();
        udpReceiver.start();
        udpReceiver.start();
        udpReceiver.start();
        assertTrue(udpReceiver.isRunning());

        String testMessage = "Hello World";
        byte[] testMessageBytes = testMessage.getBytes();

        sendUdpPacket(testMessageBytes, testPort);

        Thread.sleep(1000);

        assertEquals(1, outputQueue.size());
        assertArrayEquals(outputQueue.poll(2, TimeUnit.SECONDS), testMessageBytes);

        udpReceiver.stop();
        assertFalse(udpReceiver.isRunning());
    }

    @Test
    void testReceiveUdpPacket() throws Exception {
        udpReceiver.start();

        String testMessage = "Hello UDP!";
        byte[] testData = testMessage.getBytes();

        sendUdpPacket(testData, testPort);

        byte[] receivedData = outputQueue.poll(2, TimeUnit.SECONDS);

        assertNotNull(receivedData);
        assertArrayEquals(testData, receivedData);
    }

    private void sendUdpPacket(byte[] testData, int testPort) {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress address = InetAddress.getLocalHost();
            DatagramPacket packet = new DatagramPacket(testData, testData.length, address, testPort);
            socket.send(packet);
        } catch (Exception e) {
            System.out.println("Error while sending an UDP packet: " + e);
        }
    }
}
