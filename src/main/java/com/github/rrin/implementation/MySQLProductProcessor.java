package com.github.rrin.implementation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rrin.dto.*;
import com.github.rrin.dto.AddProductToGroup;
import com.github.rrin.dto.group.*;
import com.github.rrin.dto.product.*;
import com.github.rrin.implementation.db.WarehouseService;
import com.github.rrin.interfaces.IProcessor;
import com.github.rrin.util.CommandType;
import com.github.rrin.util.ProductSearchFilters;
import com.github.rrin.util.SearchResult;
import com.github.rrin.util.data.DataPacket;
import com.github.rrin.util.data.RequestData;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class MySQLProductProcessor implements IProcessor, Runnable {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BlockingQueue<DataPacket<Object>> inputQueue;
    private final BlockingQueue<RequestData> outputQueue;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread processorThread;

    //MySQL database manager
    WarehouseService warehouseService;

    public MySQLProductProcessor(BlockingQueue<DataPacket<Object>> inputQueue, BlockingQueue<RequestData> outputQueue) {
        this.inputQueue = inputQueue;
        this.outputQueue = outputQueue;
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            try {
                this.warehouseService = new WarehouseService();
            } catch (SQLException e) {
                System.err.println(e.getMessage());
                stop();
                return;
            }
            processorThread = new Thread(this, "ProductProcessor");
            processorThread.start();
            System.out.println("ProductProcessor started");
        }
    }

    @Override
    public void stop() {
        running.set(false);
        try {
            if (warehouseService != null) { warehouseService.close(); }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
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
                // Product CRUD operations
                case CREATE_PRODUCT -> {
                    CreateProduct p = convertValue(data, CreateProduct.class);
                    yield handleCreateProduct(p);
                }
                case GET_PRODUCT -> {
                    GetProduct p = convertValue(data, GetProduct.class);
                    yield handleGetProduct(p);
                }
                case UPDATE_PRODUCT -> {
                    UpdateProduct p = convertValue(data, UpdateProduct.class);
                    yield handleUpdateProduct(p);
                }
                case DELETE_PRODUCT -> {
                    DeleteProduct p = convertValue(data, DeleteProduct.class);
                    yield handleDeleteProduct(p);
                }
                case GET_ALL_PRODUCTS -> handleGetAllProducts();
                case SEARCH_PRODUCTS -> {
                    SearchProducts s = convertValue(data, SearchProducts.class);
                    yield handleSearchProducts(s);
                }

                // Group CRUD operations
                case CREATE_GROUP -> {
                    CreateGroup g = convertValue(data, CreateGroup.class);
                    yield handleCreateGroup(g);
                }
                case GET_GROUP -> {
                    GetGroup g = convertValue(data, GetGroup.class);
                    yield handleGetGroup(g);
                }
                case UPDATE_GROUP -> {
                    UpdateGroup g = convertValue(data, UpdateGroup.class);
                    yield handleUpdateGroup(g);
                }
                case DELETE_GROUP -> {
                    DeleteGroup g = convertValue(data, DeleteGroup.class);
                    yield handleDeleteGroup(g);
                }
                case GET_ALL_GROUPS -> handleGetAllGroups();

                // Product-Group operations
                case ADD_PRODUCT_TO_GROUP -> {
                    AddProductToGroup a = convertValue(data, AddProductToGroup.class);
                    yield handleAddProductToGroup(a);
                }
                case REMOVE_PRODUCT_FROM_GROUP -> {
                    RemoveProductFromGroup r = convertValue(data, RemoveProductFromGroup.class);
                    yield handleRemoveProductFromGroup(r);
                }
                case GET_PRODUCT_GROUPS -> {
                    GetProductGroups g = convertValue(data, GetProductGroups.class);
                    yield handleGetProductGroups(g);
                }
                case GET_GROUP_PRODUCTS -> {
                    GetGroupProducts g = convertValue(data, GetGroupProducts.class);
                    yield handleGetGroupProducts(g);
                }

                case IS_RUNNING -> {
                    IsRunning r = convertValue(data, IsRunning.class);
                    yield handleIsRunning(r);
                }

                case CLEAR_DB -> handleClearDatabase();

                default -> throw new IllegalStateException("Unexpected command type: " + command);
            };

        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
            e.printStackTrace();
            return new CommandResponse(500, "Error", "An error occurred while processing data: " + e.getMessage());
        }
    }

    private CommandResponse handleClearDatabase() {
        warehouseService.clearDB();
        return new CommandResponse(200, "Success", "DB was cleared.");
    }

    private CommandResponse handleIsRunning(IsRunning r) {
        return new CommandResponse(200, "Success", "The server is running.");
    }

    private CommandResponse handleCreateProduct(CreateProduct data) throws JsonProcessingException {
        int id = warehouseService.createProduct(data.name(), data.manufacturer(), data.description(), data.price(), data.quantity());
        if (id > 0) {
            Product product = new Product(id, data.name(),  data.manufacturer(), data.description(), data.price(), data.quantity());
            String message = String.format("Created product: %s (ID: %d)", data.name(), id);
            System.out.println(message);
            return new CommandResponse(201, "Created", objectMapper.writeValueAsString(product));
        } else {
            return new CommandResponse(400, "Error", "Failed to create product");
        }
    }

    private CommandResponse handleGetProduct(GetProduct data) throws JsonProcessingException {
        Product product = warehouseService.getProduct(data.id());
        if (product != null) {
            return new CommandResponse(200, "Success", objectMapper.writeValueAsString(product));
        } else {
            return new CommandResponse(404, "Not Found", "Product not found with ID: " + data.id());
        }
    }

    private CommandResponse handleUpdateProduct(UpdateProduct data) {
        if (!warehouseService.doProductExist(data.id())) {
            return new CommandResponse(404, "Not Found", "Product not found with ID: " + data.id());
        }

        boolean success = true;
        StringBuilder message = new StringBuilder("Updated product (ID: " + data.id() + "): ");

        if (data.name() != null) {
            boolean nameUpdated = warehouseService.setProductName(data.id(), data.name());
            success = success && nameUpdated;
            if (nameUpdated) message.append("name=").append(data.name()).append(" ");
        }

        if (data.price() != null) {
            boolean priceUpdated = warehouseService.setProductPrice(data.id(), data.price());
            success = success && priceUpdated;
            if (priceUpdated) message.append("price=").append(data.price()).append(" ");
        }

        if (data.quantity() != null) {
            boolean quantityUpdated = warehouseService.setProductQuantities(data.id(), data.quantity());
            success = success && quantityUpdated;
            if (quantityUpdated) message.append("quantity=").append(data.quantity()).append(" ");
        }

        if (data.manufacturer() != null) {
            boolean quantityUpdated = warehouseService.setProductManufacturer(data.id(), data.manufacturer());
            success = success && quantityUpdated;
            if (quantityUpdated) message.append("manufacturer=").append(data.quantity()).append(" ");
        }

        if (data.description() != null) {
            boolean quantityUpdated = warehouseService.setProductDescription(data.id(), data.description());
            success = success && quantityUpdated;
            if (quantityUpdated) message.append("description=").append(data.quantity()).append(" ");
        }

        if (success) {
            Product updatedProduct = warehouseService.getProduct(data.id());
            System.out.println(message);
            return new CommandResponse(200, "Success", message +"\n"+ updatedProduct.toFormatted());
        } else {
            return new CommandResponse(400, "Error", "Failed to update product");
        }
    }

    private CommandResponse handleDeleteProduct(DeleteProduct data) {
        Product deletedProduct = warehouseService.removeProduct(data.id());
        if (deletedProduct != null) {
            String message = String.format("Deleted product: %s (ID: %d)", deletedProduct.name(), data.id());
            System.out.println(message);
            return new CommandResponse(200, "Success", message);
        } else {
            return new CommandResponse(404, "Not Found", "Product not found with ID: " + data.id());
        }
    }

    private CommandResponse handleGetAllProducts() throws JsonProcessingException {
        List<Product> products = warehouseService.getProducts();
        return new CommandResponse(200, "Success", objectMapper.writeValueAsString(products));
    }

    private CommandResponse handleSearchProducts(SearchProducts data) throws JsonProcessingException {
        ProductSearchFilters filters = new ProductSearchFilters(
                data.name(),
                data.groupId(),
                data.minPrice(),
                data.maxPrice(),
                data.minQuantity(),
                data.maxQuantity(),
                data.page(),
                data.pageSize()
        );

        SearchResult<Product> searchResult = warehouseService.searchProducts(filters);
        SearchResult<Product> response = new SearchResult<>(
                searchResult.getItems(),
                searchResult.getTotalCount(),
                searchResult.getCurrentPage(),
                searchResult.getPageSize()
        );

        String message = String.format("Found %d products (page %d of %d)",
                searchResult.getTotalCount(),
                searchResult.getCurrentPage() + 1,
                response.getTotalPages());

        return new CommandResponse(200, "Success", objectMapper.writeValueAsString(response));
    }

    // Group CRUD handlers
    private CommandResponse handleCreateGroup(CreateGroup data) throws JsonProcessingException {
        int id = warehouseService.createGroup(data.groupName(), data.description());
        if (id > 0) {
            String message = String.format("Created group: %s (ID: %d)", data.groupName(), id);
            System.out.println(message);
            return new CommandResponse(201, "Created", objectMapper.writeValueAsString(id));
        } else {
            return new CommandResponse(400, "Error", "Failed to create group");
        }
    }

    private CommandResponse handleGetGroup(GetGroup data) throws JsonProcessingException {
        Group group = warehouseService.getGroup(data.id());
        if (group != null) {
            return new CommandResponse(200, "Success", objectMapper.writeValueAsString(group));
        } else {
            return new CommandResponse(404, "Not Found", "Group not found with ID: " + data.id());
        }
    }

    private CommandResponse handleUpdateGroup(UpdateGroup data) {
        boolean success = warehouseService.setGroupName(data.id(), data.name());
        success = success || warehouseService.setGroupDescription(data.id(), data.description());
        if (success) {
            Group updatedGroup = warehouseService.getGroup(data.id());
            String message = String.format("Updated group (ID: %d): name=%s", data.id(), data.name());
            System.out.println(message);
            return new CommandResponse(200, "Success", message);
        } else {
            return new CommandResponse(404, "Not Found", "Group not found with ID: " + data.id());
        }
    }

    private CommandResponse handleDeleteGroup(DeleteGroup data) {
        Group deletedGroup = warehouseService.removeGroup(data.id());
        if (deletedGroup != null) {
            String message = String.format("Deleted group: %s (ID: %d)", deletedGroup.name(), data.id());
            System.out.println(message);
            return new CommandResponse(200, "Success", message);
        } else {
            return new CommandResponse(404, "Not Found", "Group not found with ID: " + data.id());
        }
    }

    private CommandResponse handleGetAllGroups() throws JsonProcessingException {
        List<Group> groups = warehouseService.getGroups();
        return new CommandResponse(200, "Success", objectMapper.writeValueAsString(groups));
    }

    // Product-Group relationship handlers
    private CommandResponse handleAddProductToGroup(AddProductToGroup data) {
        boolean success = warehouseService.addProductToGroup(data.groupId(), data.productId());
        if (success) {
            String message = String.format("Added product (ID: %d) to group (ID: %d)",
                    data.productId(), data.groupId());
            System.out.println(message);
            return new CommandResponse(200, "Success", message);
        } else {
            return new CommandResponse(400, "Error",
                    "Failed to add product to group. Check if both exist and product is not already in group.");
        }
    }

    private CommandResponse handleRemoveProductFromGroup(RemoveProductFromGroup data) {
        boolean success = warehouseService.removeProductFromGroup(data.groupId(), data.productId());
        if (success) {
            String message = String.format("Removed product (ID: %d) from group (ID: %d)",
                    data.productId(), data.groupId());
            System.out.println(message);
            return new CommandResponse(200, "Success", message);
        } else {
            return new CommandResponse(400, "Error",
                    "Failed to remove product from group. Check if both exist.");
        }
    }

    private CommandResponse handleGetProductGroups(GetProductGroups data) throws JsonProcessingException {
        List<Group> groups = warehouseService.getProductGroups(data.productId());
        return new CommandResponse(200, "Success", objectMapper.writeValueAsString(groups));
    }

    private CommandResponse handleGetGroupProducts(GetGroupProducts data) throws JsonProcessingException {
        List<Product> products = warehouseService.getGroupProducts(data.groupId());
        return new CommandResponse(200, "Success", objectMapper.writeValueAsString(products));
    }

    private <T> T convertValue(Object data, Class<T> clazz) {
        return objectMapper.convertValue(data, clazz);
    }
}