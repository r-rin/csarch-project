package com.github.rrin.implementation.http.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.rrin.implementation.db.AuthService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LoginHandler extends BaseHandler {

    private final AuthService authService;

    public LoginHandler(AuthService authService) {
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

            String login = json.get("username").asText();
            String password = json.get("password").asText();

            String token = authService.loginUser(login, password);

            if (token != null) {
                Map<String, String> response = new HashMap<>();
                response.put("token", token);
                String responseJson = objectMapper.writeValueAsString(response);
                sendJsonResponse(exchange, 200, responseJson);
            } else {
                sendResponse(exchange, 401, "Unauthorized");
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 400, "Bad Request");
        }
    }
}
