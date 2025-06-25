package com.github.rrin.implementation.http.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.rrin.implementation.db.AuthService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RegisterHandler extends BaseHandler {

    private final AuthService authService;

    public RegisterHandler(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }

        try {
            String requestBody = readRequestBody(exchange);
            JsonNode json = objectMapper.readTree(requestBody);

            String username = json.get("username").asText();
            String password = json.get("password").asText();

            boolean success = authService.registerUser(username, password);

            if (success) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "User registered successfully");
                String responseJson = objectMapper.writeValueAsString(response);
                sendJsonResponse(exchange, 201, responseJson);
            } else {
                Map<String, String> response = new HashMap<>();
                response.put("error", "User already exists or invalid data");
                String responseJson = objectMapper.writeValueAsString(response);
                sendJsonResponse(exchange, 409, responseJson);
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 400, "Bad Request");
        }
    }
}
