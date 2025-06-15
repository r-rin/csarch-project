package com.github.rrin.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rrin.util.data.DataPacket;
import com.github.rrin.dto.*;
import com.github.rrin.interfaces.IProcessor;
import com.github.rrin.util.CommandType;
import com.github.rrin.util.data.RequestData;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Processor implements IProcessor, Runnable {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BlockingQueue<DataPacket<Object>> inputQueue;
    private final BlockingQueue<RequestData> outputQueue;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread processorThread;

    // Data structures to store information about products
    private final ConcurrentHashMap<String, AtomicInteger> productQuantities = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Double> productPrices = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> productGroups = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public Processor(BlockingQueue<DataPacket<Object>> inputQueue, BlockingQueue<RequestData> outputQueue) {
        this.inputQueue = inputQueue;
        this.outputQueue = outputQueue;
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            processorThread = new Thread(this, "ProductProcessor");
            processorThread.start();
            System.out.println("ProductProcessor started");
        }
    }

    @Override
    public void stop() {
        running.set(false);
        if (processorThread != null) {
            processorThread.interrupt();
        }
        System.out.println("ProductProcessor stopped");
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public void run() {
        while (running.get()) {
            try {
                DataPacket<Object> message = inputQueue.take();
                CommandResponse response = processMessage(message);
                RequestData data = new RequestData(message.getSourceId(), message.getPacketId(), message.getBody().getUserId(), response, message.getConnectionId());
                outputQueue.put(data);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Error processing message: " + e.getMessage());
            }
        }
    }

    private CommandResponse processMessage(DataPacket<?> message) {
        try {
            Object data = message.getBody().getData();
            CommandType command = message.getBody().getCommand();

            return switch (command) {
                case QUERY_QUANTITY -> {
                    QueryQuantity q = convertValue(data, QueryQuantity.class);
                    yield handleQueryQuantity(q);
                }
                case REMOVE_GOODS -> {
                    ModifyGoods g = convertValue(data, ModifyGoods.class);
                    yield handleRemoveGoods(g);
                }
                case ADD_GOODS -> {
                    ModifyGoods g = convertValue(data, ModifyGoods.class);
                    yield handleAddGoods(g);
                }
                case ADD_GROUP -> {
                    CreateGroup g = convertValue(data, CreateGroup.class);
                    yield handleAddGroup(g);
                }
                case ADD_PRODUCT_TO_GROUP -> {
                    AddProductToGroup g = convertValue(data, AddProductToGroup.class);
                    yield handleAddProductToGroup(g);
                }
                case SET_PRICE -> {
                    CreateProduct p = convertValue(data, CreateProduct.class);
                    yield handleSetPrice(p);
                }
                default -> throw new IllegalStateException("Unexpected command type: " + command);
            };

        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
            return new CommandResponse(0, "Error", "An error occurred while processing data: " + e.getMessage());
        }
    }

    private CommandResponse handleQueryQuantity(Object data) {
        if (data instanceof QueryQuantity(String productName)) {
            AtomicInteger quantity = productQuantities.get(productName);
            if (quantity == null) {
                throw new IllegalArgumentException("No such product: " + productName);
            }

            int result = quantity.get();
            String message = String.format("Quantity for product \"%s\" is %s", productName, result);
            System.out.println(message);
            return new CommandResponse(200, "Success!", message);
        }
        throw new IllegalArgumentException("Invalid data for QUERY_QUANTITY");
    }

    private CommandResponse handleRemoveGoods(Object data) {
        if (data instanceof ModifyGoods(String productName, int amount)) {
            lock.writeLock().lock();
            try {
                AtomicInteger quantity = productQuantities.get(productName);

                if (quantity == null) {
                    throw new IllegalArgumentException("No such product: " + productName);
                }

                if (amount < 1) throw new IllegalArgumentException("Invalid amount for REMOVE_GOODS: " + amount);

                int currentQuantity = quantity.get();
                if (currentQuantity >= amount) {
                    quantity.addAndGet(-amount);
                    String message = String.format(
                            "Removed %d of %s. New quantity: %d",
                            amount, productName, quantity.get());

                    System.out.println(message);
                    return new CommandResponse(200, "Success!", message);
                } else {
                    throw new IllegalStateException("Amount to subtract is too big. Available: " + currentQuantity + ", requested: " + amount);
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
        throw new IllegalArgumentException("Invalid data for REMOVE_GOODS");
    }

    private CommandResponse handleAddGoods(Object data) {
        if (data instanceof ModifyGoods(String productName, int amount)) {
            AtomicInteger quantity = productQuantities.computeIfAbsent(productName,
                    k -> new AtomicInteger(0));

            if (amount < 1) throw new IllegalArgumentException("Invalid amount for ADD_GOODS: " + amount);

            int newQuantity = quantity.addAndGet(amount);
            productPrices.putIfAbsent(productName, 100.0);

            String message = String.format("Added %d of %s. New quantity: %d", amount, productName, newQuantity);
            System.out.println(message);
            return new CommandResponse(200, "Success!", message);
        }
        throw new IllegalArgumentException("Invalid data for ADD_GOODS");
    }

    private CommandResponse handleAddGroup(Object data) {
        if (data instanceof CreateGroup(String groupName)) {
            productGroups.putIfAbsent(groupName, ConcurrentHashMap.newKeySet());
            String message = String.format("Added group \"%s\"", groupName);
            System.out.println(message);
            return new CommandResponse(200, "Success!", message);
        }
        throw new IllegalArgumentException("Invalid data for ADD_GROUP");
    }

    private CommandResponse handleAddProductToGroup(Object data) {
        if (data instanceof AddProductToGroup(String productName, String groupName)) {
            Set<String> group = productGroups.get(groupName);
            if (group != null) {
                boolean isSuccess = group.add(productName);
                if (!isSuccess) {
                    throw  new IllegalStateException(
                            String.format("Product \"%s\" already exists in group \"%s\"", productName, groupName)
                    );
                }

                String message = String.format("Added product \"%s\" to \"%s\"", productName, groupName);
                System.out.println(message);
                return new CommandResponse(200, "Success!", message);
            } else {
                throw new IllegalArgumentException("Group not found: " + groupName);
            }
        }
        throw new IllegalArgumentException("Invalid data for ADD_PRODUCT_TO_GROUP");
    }

    private CommandResponse handleSetPrice(Object data) {
        if (data instanceof CreateProduct(String name, double price)) {
            if (!productQuantities.containsKey(name)) {
                throw new IllegalArgumentException("Product does not exist: " + name);
            }

            productPrices.put(name, price);
            String message = String.format("Set price for product \"%s\" with value \"%s\"", name, price);
            System.out.println(message);
            return new CommandResponse(200, "Success!", message);
        }
        throw new IllegalArgumentException("Invalid data for SET_PRICE");
    }

    private <T> T convertValue(Object data, Class<T> clazz) {
        return objectMapper.convertValue(data, clazz);
    }
}