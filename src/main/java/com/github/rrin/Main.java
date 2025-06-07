package com.github.rrin;

import com.github.rrin.implementation.UdpReceiver;
import com.github.rrin.util.MockClient;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;

public class Main {
    public static void main(String[] args) {
        String serverHost = "127.0.0.1";
        int serverPort = 5555;

        UdpReceiver receiver = new UdpReceiver(new ArrayBlockingQueue<>(100), serverPort);
        MockClient client = new MockClient(serverHost, serverPort);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n!!! Stopping test client !!!");
            client.stop();
            receiver.stop();
        }));

        try {
            receiver.start();
            client.start();

            while (client.isRunning()) {
                Thread.sleep(1000);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            client.stop();
        }
    }

    public static String bytesToHex(byte[] bytes) {
        final byte[] hexArray = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }
}