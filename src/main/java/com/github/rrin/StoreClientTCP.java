package com.github.rrin;

import com.github.rrin.dto.*;
import com.github.rrin.util.CommandType;
import com.github.rrin.util.Converter;
import com.github.rrin.util.DelayedRequest;
import com.github.rrin.util.data.DataPacket;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class StoreClientTCP {
    private final String serverHost;
    private final int serverPort;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean serverAvailable = new AtomicBoolean(false);
    private final AtomicLong packetIdCounter = new AtomicLong(1);

    private final ConcurrentHashMap<Long, CompletableFuture<DataPacket<CommandResponse>>> pendingRequests = new ConcurrentHashMap<>();
    private final BlockingQueue<DelayedRequest> requestQueue = new LinkedBlockingQueue<>();

    private Socket socket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private Thread receiverThread;
    private Thread senderThread;
    private Thread reconnectThread;

    private static final int DEFAULT_TIMEOUT_SECONDS = 10;
    private static final int CONNECTION_TIMEOUT_MS = 5000;
    private static final int RECONNECT_INTERVAL_MS = 5000;
    private static final int SOCKET_TIMEOUT_MS = 1000;

    public StoreClientTCP(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    public void start() {
        if (running.compareAndSet(false, true)) {

            reconnectThread = new Thread(this::runReconnectLoop, "StoreClient-Reconnect");
            reconnectThread.start();

            senderThread = new Thread(this::runSender, "StoreClient-Sender");
            senderThread.start();

            System.out.println("StoreClientTCP started, connecting to " + serverHost + ":" + serverPort);
        }
    }

    public void stop() {
        if (running.compareAndSet(true, false)) {
            System.out.println("Stopping StoreClientTCP...");

            pendingRequests.values().forEach(future ->
                    future.completeExceptionally(new RuntimeException("Client stopped")));
            pendingRequests.clear();

            requestQueue.clear();

            closeConnection();

            if (reconnectThread != null) {
                reconnectThread.interrupt();
                try {
                    reconnectThread.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            if (senderThread != null) {
                senderThread.interrupt();
                try {
                    senderThread.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            if (receiverThread != null) {
                receiverThread.interrupt();
                try {
                    receiverThread.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            System.out.println("StoreClientTCP stopped");
        }
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
            System.out.println("Request timed out after " + timeoutSeconds + " seconds");
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
        CompletableFuture<DataPacket<CommandResponse>> future = new CompletableFuture<>();

        pendingRequests.put(packetId, future);

        DelayedRequest request = new DelayedRequest(command, data, future, packetId);

        if (!requestQueue.offer(request)) {
            pendingRequests.remove(packetId);
            future.completeExceptionally(new RuntimeException("Request queue is full"));
        }

        return future;
    }

    private void connectToServer() {
        try {
            closeConnection();

            socket = new Socket();
            socket.connect(new InetSocketAddress(serverHost, serverPort), CONNECTION_TIMEOUT_MS);
            socket.setSoTimeout(SOCKET_TIMEOUT_MS);

            inputStream = new DataInputStream(socket.getInputStream());
            outputStream = new DataOutputStream(socket.getOutputStream());

            if (receiverThread != null) {
                receiverThread.interrupt();
            }
            receiverThread = new Thread(this::runReceiver, "StoreClient-Receiver");
            receiverThread.start();

            serverAvailable.set(true);
            System.out.println("[STORE-CLIENT] Connected to server " + serverHost + ":" + serverPort);

        } catch (Exception e) {
            System.out.println("[STORE-CLIENT] Failed to connect to server: " + e.getMessage());
            closeConnection();
            serverAvailable.set(false);
        }
    }

    private void closeConnection() {
        serverAvailable.set(false);

        try {
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (IOException ignored) {}

        try {
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException ignored) {}

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {}

        outputStream = null;
        inputStream = null;
        socket = null;
    }

    private void runReconnectLoop() {
        while (running.get()) {
            if (!serverAvailable.get()) {
                System.out.println("[STORE-CLIENT] Attempting to reconnect...");
                connectToServer();
            }

            try {
                Thread.sleep(RECONNECT_INTERVAL_MS);
            } catch (InterruptedException e) {
                break;
            }
        }
        System.out.println("[STORE-CLIENT] Reconnect thread stopped");
    }

    private void runSender() {
        while (running.get()) {
            try {
                DelayedRequest request = requestQueue.poll(1, TimeUnit.SECONDS);
                if (request == null) {
                    continue;
                }

                if (!serverAvailable.get()) {
                    requestQueue.offer(request);
                    Thread.sleep(100);
                    continue;
                }

                try {
                    sendPacketNow(request);
                } catch (Exception e) {
                    System.err.println("[STORE-CLIENT] Error sending packet: " + e.getMessage());
                    serverAvailable.set(false);
                    requestQueue.offer(request);
                }

            } catch (InterruptedException e) {
                break;
            }
        }
        System.out.println("[STORE-CLIENT] Sender thread stopped");
    }

    private void sendPacketNow(DelayedRequest request) throws Exception {
        DataPacket<Object> requestPacket = new DataPacket<>(
                (byte) 0x13,
                (byte) 1,
                request.packetId(),
                request.command(),
                1,
                request.data(),
                0
        );

        byte[] packetBytes = requestPacket.toByteArray();
        outputStream.write(packetBytes);
        outputStream.flush();

        System.out.println("[STORE-CLIENT] Sent " + request.command() +
                " request (ID: " + request.packetId() + ")");
        System.out.println("[STORE-CLIENT] Data: " + Converter.bytesToHex(packetBytes));
    }

    private void runReceiver() {
        while (running.get() && serverAvailable.get()) {
            try {
                byte[] buffer = new byte[2048];
                inputStream.read(buffer);
                handleReceivedPacket(buffer);
            } catch (SocketTimeoutException e) {
                continue;
            } catch (Exception e) {
                if (running.get() && serverAvailable.get()) {
                    System.err.println("[STORE-CLIENT] Error receiving: " + e.getMessage());
                    serverAvailable.set(false);
                }
                break;
            }
        }
        System.out.println("[STORE-CLIENT] Receiver thread stopped");
    }

    private void handleReceivedPacket(byte[] data) {
        try {
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

    public boolean isServerAvailable() {
        return serverAvailable.get();
    }

    public int getPendingRequestCount() {
        return pendingRequests.size();
    }

    public int getQueuedRequestCount() {
        return requestQueue.size();
    }

    public void printStatistics() {
        System.out.println("\n[STORE-CLIENT] Running: " + running.get());
        System.out.println("[STORE-CLIENT] Server available: " + serverAvailable.get());
        System.out.println("[STORE-CLIENT] Pending requests: " + pendingRequests.size());
        System.out.println("[STORE-CLIENT] Queued requests: " + requestQueue.size());
        System.out.println("[STORE-CLIENT] Next packet ID: " + packetIdCounter.get());
    }
}
