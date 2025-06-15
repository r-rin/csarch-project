package com.github.rrin;

import com.github.rrin.dto.*;
import com.github.rrin.util.CommandType;
import com.github.rrin.util.Converter;
import com.github.rrin.util.data.DataPacket;

import java.net.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class StoreClientUDP {
    private final String serverHost;
    private final int serverPort;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong packetIdCounter = new AtomicLong(1);

    private final ConcurrentHashMap<Long, CompletableFuture<DataPacket<CommandResponse>>> pendingRequests = new ConcurrentHashMap<>();

    private DatagramSocket socket;
    private Thread receiverThread;

    private static final int DEFAULT_TIMEOUT_SECONDS = 10;
    private static final int SOCKET_TIMEOUT_MS = 1000;

    public StoreClientUDP(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            try {
                socket = new DatagramSocket();
            } catch (SocketException e) {
                throw new RuntimeException(e);
            }
            try {
                socket.setSoTimeout(SOCKET_TIMEOUT_MS);
            } catch (SocketException e) {
                throw new RuntimeException(e);
            }

            receiverThread = new Thread(this::runReceiver, "StoreClient-Receiver");
            receiverThread.start();

            System.out.println("StoreClient connected to " + serverHost + ":" + serverPort);
        }
    }

    public void stop() {
        if (running.compareAndSet(true, false)) {
            System.out.println("Stopping StoreClient...");

            pendingRequests.values().forEach(future ->
                    future.completeExceptionally(new RuntimeException("Client stopped")));
            pendingRequests.clear();

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

            if (receiverThread != null) {
                receiverThread.interrupt();
                try {
                    receiverThread.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            System.out.println("StoreClient stopped");
        }
    }

    public DataPacket<CommandResponse> isServerRunning() throws Exception {
        IsRunning request = new IsRunning();
        return sendRequestAndWait(CommandType.IS_RUNNING, request, DEFAULT_TIMEOUT_SECONDS);
    }

    public DataPacket<CommandResponse> queryQuantity(String productName) throws Exception {
        return queryQuantity(productName, DEFAULT_TIMEOUT_SECONDS);
    }

    public DataPacket<CommandResponse> addGoods(String productName, int quantity) throws Exception {
        return addGoods(productName, quantity, DEFAULT_TIMEOUT_SECONDS);
    }

    public DataPacket<CommandResponse> removeGoods(String productName, int quantity) throws Exception {
        return removeGoods(productName, quantity, DEFAULT_TIMEOUT_SECONDS);
    }

    public DataPacket<CommandResponse> createGroup(String groupName) throws Exception {
        return createGroup(groupName, DEFAULT_TIMEOUT_SECONDS);
    }

    public DataPacket<CommandResponse> addProductToGroup(String productName, String groupName) throws Exception {
        return addProductToGroup(productName, groupName, DEFAULT_TIMEOUT_SECONDS);
    }

    public DataPacket<CommandResponse> setPrice(String productName, double price) throws Exception {
        return setPrice(productName, price, DEFAULT_TIMEOUT_SECONDS);
    }

    public DataPacket<CommandResponse> queryQuantity(String productName, int timeoutSeconds) throws Exception {
        QueryQuantity query = new QueryQuantity(productName);
        return sendRequestAndWait(CommandType.QUERY_QUANTITY, query, timeoutSeconds);
    }

    public DataPacket<CommandResponse> addGoods(String productName, int quantity, int timeoutSeconds) throws Exception {
        ModifyGoods request = new ModifyGoods(productName, quantity);
        return sendRequestAndWait(CommandType.ADD_GOODS, request, timeoutSeconds);
    }

    public DataPacket<CommandResponse> removeGoods(String productName, int quantity, int timeoutSeconds) throws Exception {
        ModifyGoods request = new ModifyGoods(productName, quantity);
        return sendRequestAndWait(CommandType.REMOVE_GOODS, request, timeoutSeconds);
    }

    public DataPacket<CommandResponse> createGroup(String groupName, int timeoutSeconds) throws Exception {
        CreateGroup request = new CreateGroup(groupName);
        return sendRequestAndWait(CommandType.ADD_GROUP, request, timeoutSeconds);
    }

    public DataPacket<CommandResponse> addProductToGroup(String productName, String groupName, int timeoutSeconds) throws Exception {
        AddProductToGroup request = new AddProductToGroup(productName, groupName);
        return sendRequestAndWait(CommandType.ADD_PRODUCT_TO_GROUP, request, timeoutSeconds);
    }

    public DataPacket<CommandResponse> setPrice(String productName, double price, int timeoutSeconds) throws Exception {
        CreateProduct request = new CreateProduct(productName, price);
        return sendRequestAndWait(CommandType.SET_PRICE, request, timeoutSeconds);
    }

    private DataPacket<CommandResponse> sendRequestAndWait(CommandType command, Object data, int timeoutSeconds) throws Exception {
        CompletableFuture<DataPacket<CommandResponse>> future = sendRequestAsync(command, data);

        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            System.out.println("Packet with ID: " + (packetIdCounter.get() - 1) + " was timed out!");
            return null;
        }
    }

    private CompletableFuture<DataPacket<CommandResponse>> sendRequestAsync(CommandType command, Object data) {
        if (!running.get()) {
            CompletableFuture<DataPacket<CommandResponse>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new IllegalStateException("Client is not running"));
            return failedFuture;
        }

        long packetId = packetIdCounter.getAndIncrement();
        DataPacket<Object> requestPacket = new DataPacket<>(
                (byte) 0x13,
                (byte) 1,
                packetId,
                command,
                1,
                data,
                0
        );

        CompletableFuture<DataPacket<CommandResponse>> future = new CompletableFuture<>();
        pendingRequests.put(packetId, future);

        try {
            sendPacket(requestPacket);
        } catch (Exception e) {
            pendingRequests.remove(packetId);
            future.completeExceptionally(e);
        }

        return future;
    }

    private void sendPacket(DataPacket<?> packet) throws Exception {
        byte[] packetBytes = packet.toByteArray();

        InetAddress serverAddress = InetAddress.getByName(serverHost);
        DatagramPacket udpPacket = new DatagramPacket(
                packetBytes,
                packetBytes.length,
                serverAddress,
                serverPort
        );

        socket.send(udpPacket);

        System.out.println("[STORE-CLIENT] Sent " + packet.getBody().getCommand() +
                " request (ID: " + packet.getPacketId() + ")");
        System.out.println("[STORE-CLIENT] Data: " + Converter.bytesToHex(packetBytes));
    }

    private void runReceiver() {
        byte[] buffer = new byte[4096];

        while (running.get()) {
            printStatistics();
            try {
                DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(receivePacket);

                handleReceivedPacket(receivePacket);

            } catch (SocketTimeoutException e) {
                continue;
            } catch (Exception e) {
                if (running.get()) {
                    System.err.println("[STORE-CLIENT] Error receiving: " + e.getMessage());
                }
            }
        }

        System.out.println("[STORE-CLIENT] Receiver thread stopped");
    }

    private void handleReceivedPacket(DatagramPacket receivePacket) {
        try {
            byte[] data = new byte[receivePacket.getLength()];
            System.arraycopy(receivePacket.getData(), receivePacket.getOffset(), data, 0, receivePacket.getLength());

            System.out.println("[STORE-CLIENT] Received response (" + data.length + " bytes)");
            System.out.println("[STORE-CLIENT] Data: " + Converter.bytesToHex(data));

            DataPacket<CommandResponse> responsePacket = DataPacket.fromByteArray(data, CommandResponse.class, 0);
            long packetId = responsePacket.getPacketId();

            CompletableFuture<DataPacket<CommandResponse>> future = pendingRequests.remove(packetId);
            if (future != null) {
                CommandResponse res = responsePacket.getBody().getData();
                System.out.printf("""
                        [STORE-CLIENT] Response received for request ID: %d
                          - Status Code : %s
                          - Title       : %s
                          - Message     : %s
                        %n""", packetId, res.statusCode(), res.title(), res.message());

                future.complete(responsePacket);
            } else {
                System.out.println("[STORE-CLIENT] Received unexpected response for ID: " + packetId);
            }

        } catch (Exception e) {
            System.err.println("[STORE-CLIENT] Error processing response: " + e.getMessage());
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    public int getPendingRequestCount() {
        return pendingRequests.size();
    }

    public void printStatistics() {
        System.out.println("\n[STORE-CLIENT] Running: " + running.get());
        System.out.println("[STORE-CLIENT] Pending requests: " + pendingRequests.size());
        System.out.println("[STORE-CLIENT] Next packet ID: " + packetIdCounter.get());
    }
}
