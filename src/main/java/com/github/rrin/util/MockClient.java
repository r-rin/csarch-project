package com.github.rrin.util;


import com.github.rrin.DataPacket;
import com.github.rrin.Main;
import com.github.rrin.dto.*;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class MockClient implements Runnable {
    private final String serverHost;
    private final int serverPort;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Random random = new Random();
    private DatagramSocket socket;
    private Thread clientThread;

    public MockClient(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    public boolean isRunning() {
        return running.get();
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            try {
                socket = new DatagramSocket();
                clientThread = new Thread(this, "MockClient");
                clientThread.start();
                System.out.println("MockClient started, sending to " + serverHost + ":" + serverPort);
            } catch (SocketException e) {
                running.set(false);
                throw new RuntimeException("Failed to start MockClient", e);
            }
        }
    }

    public void stop() {
        running.set(false);
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        if (clientThread != null) {
            clientThread.interrupt();
        }
        System.out.println("MockClient stopped");
    }

    @Override
    public void run() {
        long packetId = 1;

        while (running.get()) {
            try {
                // Generate random message
                DataPacket<?> packet = generateRandomPacket(packetId++);
                byte[] packetBytes = packet.toByteArray();

                // Send UDP packet
                InetAddress serverAddress = InetAddress.getByName(serverHost);
                DatagramPacket udpPacket = new DatagramPacket(
                        packetBytes,
                        packetBytes.length,
                        serverAddress,
                        serverPort
                );

                socket.send(udpPacket);
                System.out.println("Sent packet " + packet.getPacketId() + " with command " +
                        packet.getBody().getCommand() + " (" + packetBytes.length + " bytes)");
                System.out.println("Sent data: " + Main.bytesToHex(packetBytes));

                // Wait random time before next mocked packet
                Thread.sleep(1000 + random.nextInt(5000));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Error sending packet: " + e.getMessage());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private DataPacket<?> generateRandomPacket(long packetId) {
        CommandType[] commands = CommandType.values();
        CommandType randomCommand = commands[random.nextInt(commands.length)];

        byte sourceId = (byte) (1 + random.nextInt(10));
        int userId = random.nextInt(100);

        Object data = generateRandomData(randomCommand);

        return new DataPacket<>((byte) 0x13, sourceId, packetId, randomCommand, userId, data);
    }

    private Object generateRandomData(CommandType command) {
        return switch (command) {
            case QUERY_QUANTITY -> new QueryQuantity("Product" + random.nextInt(10));
            case REMOVE_GOODS -> new ModifyGoods("Product" + random.nextInt(10), random.nextInt(50) + 1);
            case ADD_GOODS -> new ModifyGoods("Product" + random.nextInt(10), random.nextInt(100) + 1);
            case ADD_GROUP -> new CreateGroup("Group" + random.nextInt(5));
            case ADD_PRODUCT_TO_GROUP -> new AddProductToGroup("Product" + random.nextInt(10), "Group" + random.nextInt(5));
            case SET_PRICE -> new CreateProduct("Product" + random.nextInt(10), 10.0 + random.nextDouble() * 990.0);
        };
    }
}