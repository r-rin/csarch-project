package com.github.rrin.implementation.http.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.rrin.dto.Product;
import com.github.rrin.implementation.db.AuthService;
import com.github.rrin.implementation.db.WarehouseService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GoodsHandler extends BaseHandler {

    private final AuthService authService;
    private final WarehouseService warehouseService;

    public GoodsHandler(AuthService authService, WarehouseService warehouseService) {
        this.authService = authService;
        this.warehouseService = warehouseService;
    }

    private boolean isAuthenticated(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }

        // Remove "Bearer " part from a token
        String token = authHeader.substring(7);
        return authService.validateToken(token);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if (!isAuthenticated(exchange)) {
            sendResponse(exchange, 403, "Forbidden");
            return;
        }

        try {
            switch (method) {
                case "GET":
                    handleGet(exchange, path);
                    break;
                case "PUT":
                    handlePut(exchange, path);
                    break;
                case "POST":
                    handlePost(exchange, path);
                    break;
                case "DELETE":
                    handleDelete(exchange, path);
                    break;
                default:
                    sendResponse(exchange, 405, "Method Not Allowed");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "Internal Server Error");
        }
    }

    private void handleGet(HttpExchange exchange, String path) throws IOException {
        String[] pathParts = path.split("/");
        if (pathParts.length != 4) {
            sendResponse(exchange, 400, "Bad Request");
            return;
        }

        try {
            int productId = Integer.parseInt(pathParts[3]);
            Product product = warehouseService.getProduct(productId);

            if (product != null) {
                Map<String, Object> productMap = new HashMap<>();
                productMap.put("id", product.id());
                productMap.put("name", product.name());
                productMap.put("price", product.price());
                productMap.put("quantity", product.quantity());

                String responseJson = objectMapper.writeValueAsString(productMap);
                sendJsonResponse(exchange, 200, responseJson);
            } else {
                sendResponse(exchange, 404, "Not Found");
            }
        } catch (NumberFormatException e) {
            sendResponse(exchange, 400, "Bad Request");
        }
    }

    private void handlePut(HttpExchange exchange, String path) throws IOException {
        // PUT /api/good - create new product
        if (!"/api/good".equals(path)) {
            sendResponse(exchange, 400, "Bad Request");
            return;
        }

        try {
            String requestBody = readRequestBody(exchange);
            JsonNode json = objectMapper.readTree(requestBody);

            String name = json.get("name").asText();
            double price = json.get("price").asDouble();
            int quantity = json.get("quantity").asInt();

            // Validate input
            if (name == null || name.trim().isEmpty() || price < 0 || quantity < 0) {
                sendResponse(exchange, 409, "Conflict");
                return;
            }

            int productId = warehouseService.createProduct(name, price, quantity);

            if (productId > 0) {
                Map<String, Integer> response = new HashMap<>();
                response.put("id", productId);
                String responseJson = objectMapper.writeValueAsString(response);
                sendJsonResponse(exchange, 201, responseJson);
            } else {
                sendResponse(exchange, 409, "Conflict");
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 409, "Conflict");
        }
    }

    private void handlePost(HttpExchange exchange, String path) throws IOException {
        // POST /api/good/{id} - update existing
        String[] pathParts = path.split("/");
        if (pathParts.length != 4) {
            sendResponse(exchange, 400, "Bad Request");
            return;
        }

        try {
            int productId = Integer.parseInt(pathParts[3]);

            // Check if product exists
            if (!warehouseService.doProductExist(productId)) {
                sendResponse(exchange, 404, "Not Found");
                return;
            }

            String requestBody = readRequestBody(exchange);
            JsonNode json = objectMapper.readTree(requestBody);

            boolean updated = false;

            // Update name if provided
            if (json.has("name")) {
                String name = json.get("name").asText();
                if (name != null && !name.trim().isEmpty()) {
                    updated = warehouseService.setProductName(productId, name) || updated;
                } else {
                    sendResponse(exchange, 409, "Conflict");
                    return;
                }
            }

            // Update price if provided
            if (json.has("price")) {
                double price = json.get("price").asDouble();
                if (price >= 0) {
                    updated = warehouseService.setProductPrice(productId, price) || updated;
                } else {
                    sendResponse(exchange, 409, "Conflict");
                    return;
                }
            }

            // Update quantity if provided
            if (json.has("quantity")) {
                int quantity = json.get("quantity").asInt();
                if (quantity >= 0) {
                    updated = warehouseService.setProductQuantities(productId, quantity) || updated;
                } else {
                    sendResponse(exchange, 409, "Conflict");
                    return;
                }
            }

            if (updated) {
                sendResponse(exchange, 204, "");
            } else {
                sendResponse(exchange, 409, "Conflict");
            }

        } catch (NumberFormatException e) {
            sendResponse(exchange, 400, "Bad Request");
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 409, "Conflict");
        }
    }

    private void handleDelete(HttpExchange exchange, String path) throws IOException {
        // DELETE /api/good/{id} - delete existing
        String[] pathParts = path.split("/");
        if (pathParts.length != 4) {
            sendResponse(exchange, 400, "Bad Request");
            return;
        }

        try {
            int productId = Integer.parseInt(pathParts[3]);
            Product removedProduct = warehouseService.removeProduct(productId);

            if (removedProduct != null) {
                sendResponse(exchange, 204, "");
            } else {
                sendResponse(exchange, 404, "Not Found");
            }

        } catch (NumberFormatException e) {
            sendResponse(exchange, 400, "Bad Request");
        }
    }
}
