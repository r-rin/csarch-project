package com.github.rrin.client;

import com.github.rrin.dto.*;
import com.github.rrin.dto.AddProductToGroup;
import com.github.rrin.dto.group.*;
import com.github.rrin.dto.product.*;
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

    // Product operations
    public DataPacket<CommandResponse> createProduct(String name, String manufacturer, String description, double price, int quantity) throws Exception {
        return createProduct(name, manufacturer, description, price, quantity, DEFAULT_TIMEOUT_SECONDS);
    }

    public DataPacket<CommandResponse> createProduct(String name, String manufacturer, String description, double price, int quantity, int timeoutSeconds) throws Exception {
        CreateProduct request = new CreateProduct(name, manufacturer, description, price, quantity);
        return sendRequestAndWait(CommandType.CREATE_PRODUCT, request, timeoutSeconds);
    }

    public DataPacket<CommandResponse> getProduct(int id) throws Exception {
        return getProduct(id, DEFAULT_TIMEOUT_SECONDS);
    }

    public DataPacket<CommandResponse> getProduct(int id, int timeoutSeconds) throws Exception {
        GetProduct request = new GetProduct(id);
        return sendRequestAndWait(CommandType.GET_PRODUCT, request, timeoutSeconds);
    }

    public DataPacket<CommandResponse> updateProduct(int id, String name, Double price, Integer quantity) throws Exception {
        return updateProduct(id, name, price, quantity, DEFAULT_TIMEOUT_SECONDS);
    }

    public DataPacket<CommandResponse> updateProduct(int id, String name, Double price, Integer quantity, int timeoutSeconds) throws Exception {
        UpdateProduct request = new UpdateProduct(id, name, price, quantity);
        return sendRequestAndWait(CommandType.UPDATE_PRODUCT, request, timeoutSeconds);
    }

    public DataPacket<CommandResponse> deleteProduct(int id) throws Exception {
        return deleteProduct(id, DEFAULT_TIMEOUT_SECONDS);
    }

    public DataPacket<CommandResponse> deleteProduct(int id, int timeoutSeconds) throws Exception {
        DeleteProduct request = new DeleteProduct(id);
        return sendRequestAndWait(CommandType.DELETE_PRODUCT, request, timeoutSeconds);
    }

    public DataPacket<CommandResponse> getAllProducts() throws Exception {
        return getAllProducts(DEFAULT_TIMEOUT_SECONDS);
    }

    public DataPacket<CommandResponse> getAllProducts(int timeoutSeconds) throws Exception {
        return sendRequestAndWait(CommandType.GET_ALL_PRODUCTS, null, timeoutSeconds);
    }

    public DataPacket<CommandResponse> searchProducts(String name, Integer groupId, Double minPrice, Double maxPrice,
                                                      Integer minQuantity, Integer maxQuantity, Integer page, Integer pageSize) throws Exception {
        return searchProducts(name, groupId, minPrice, maxPrice, minQuantity, maxQuantity, page, pageSize, DEFAULT_TIMEOUT_SECONDS);
    }

    public DataPacket<CommandResponse> searchProducts(String name, Integer groupId, Double minPrice, Double maxPrice,
                                                      Integer minQuantity, Integer maxQuantity, Integer page, Integer pageSize, int timeoutSeconds) throws Exception {
        SearchProducts request = new SearchProducts(name, groupId, minPrice, maxPrice, minQuantity, maxQuantity, page, pageSize);
        return sendRequestAndWait(CommandType.SEARCH_PRODUCTS, request, timeoutSeconds);
    }

    // Group operations
    public DataPacket<CommandResponse> createGroup(String groupName, String description) throws Exception {
        return createGroup(groupName, description, DEFAULT_TIMEOUT_SECONDS);
    }

    public DataPacket<CommandResponse> createGroup(String groupName, String descritpion, int timeoutSeconds) throws Exception {
        CreateGroup request = new CreateGroup(groupName, descritpion);
        return sendRequestAndWait(CommandType.CREATE_GROUP, request, timeoutSeconds);
    }

    public DataPacket<CommandResponse> getGroup(int id) throws Exception {
        return getGroup(id, DEFAULT_TIMEOUT_SECONDS);
    }

    public DataPacket<CommandResponse> getGroup(int id, int timeoutSeconds) throws Exception {
        GetGroup request = new GetGroup(id);
        return sendRequestAndWait(CommandType.GET_GROUP, request, timeoutSeconds);
    }

    public DataPacket<CommandResponse> updateGroup(int id, String name) throws Exception {
        return updateGroup(id, name, DEFAULT_TIMEOUT_SECONDS);
    }

    public DataPacket<CommandResponse> updateGroup(int id, String name, int timeoutSeconds) throws Exception {
        UpdateGroup request = new UpdateGroup(id, name);
        return sendRequestAndWait(CommandType.UPDATE_GROUP, request, timeoutSeconds);
    }

    public DataPacket<CommandResponse> deleteGroup(int id) throws Exception {
        return deleteGroup(id, DEFAULT_TIMEOUT_SECONDS);
    }

    public DataPacket<CommandResponse> deleteGroup(int id, int timeoutSeconds) throws Exception {
        DeleteGroup request = new DeleteGroup(id);
        return sendRequestAndWait(CommandType.DELETE_GROUP, request, timeoutSeconds);
    }

    public DataPacket<CommandResponse> getAllGroups() throws Exception {
        return getAllGroups(DEFAULT_TIMEOUT_SECONDS);
    }

    public DataPacket<CommandResponse> getAllGroups(int timeoutSeconds) throws Exception {
        return sendRequestAndWait(CommandType.GET_ALL_GROUPS, null, timeoutSeconds);
    }

    // Product-Group relationship operations
    public DataPacket<CommandResponse> addProductToGroup(int productId, int groupId) throws Exception {
        return addProductToGroup(productId, groupId, DEFAULT_TIMEOUT_SECONDS);
    }

    public DataPacket<CommandResponse> addProductToGroup(int productId, int groupId, int timeoutSeconds) throws Exception {
        AddProductToGroup request = new AddProductToGroup(productId, groupId);
        return sendRequestAndWait(CommandType.ADD_PRODUCT_TO_GROUP, request, timeoutSeconds);
    }

    public DataPacket<CommandResponse> removeProductFromGroup(int productId, int groupId) throws Exception {
        return removeProductFromGroup(productId, groupId, DEFAULT_TIMEOUT_SECONDS);
    }

    public DataPacket<CommandResponse> removeProductFromGroup(int productId, int groupId, int timeoutSeconds) throws Exception {
        RemoveProductFromGroup request = new RemoveProductFromGroup(productId, groupId);
        return sendRequestAndWait(CommandType.REMOVE_PRODUCT_FROM_GROUP, request, timeoutSeconds);
    }

    public DataPacket<CommandResponse> getProductGroups(int productId) throws Exception {
        return getProductGroups(productId, DEFAULT_TIMEOUT_SECONDS);
    }

    public DataPacket<CommandResponse> getProductGroups(int productId, int timeoutSeconds) throws Exception {
        GetProductGroups request = new GetProductGroups(productId);
        return sendRequestAndWait(CommandType.GET_PRODUCT_GROUPS, request, timeoutSeconds);
    }

    public DataPacket<CommandResponse> getGroupProducts(int groupId) throws Exception {
        return getGroupProducts(groupId, DEFAULT_TIMEOUT_SECONDS);
    }

    public DataPacket<CommandResponse> getGroupProducts(int groupId, int timeoutSeconds) throws Exception {
        GetGroupProducts request = new GetGroupProducts(groupId);
        return sendRequestAndWait(CommandType.GET_GROUP_PRODUCTS, request, timeoutSeconds);
    }

    // System operations
    public DataPacket<CommandResponse> isServerRunning() throws Exception {
        return isServerRunning(DEFAULT_TIMEOUT_SECONDS);
    }

    public DataPacket<CommandResponse> isServerRunning(int timeoutSeconds) throws Exception {
        IsRunning request = new IsRunning();
        return sendRequestAndWait(CommandType.IS_RUNNING, request, timeoutSeconds);
    }

    public DataPacket<CommandResponse> clearDatabase() throws Exception {
        return clearDatabase(DEFAULT_TIMEOUT_SECONDS);
    }

    public DataPacket<CommandResponse> clearDatabase(int timeoutSeconds) throws Exception {
        return sendRequestAndWait(CommandType.CLEAR_DB, null, timeoutSeconds);
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
