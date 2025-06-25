package com.github.rrin.implementation.http;

import com.github.rrin.implementation.http.handlers.GoodsHandler;
import com.github.rrin.implementation.http.handlers.LoginHandler;
import com.github.rrin.implementation.http.handlers.RegisterHandler;
import com.github.rrin.util.MySQLOptions;
import com.sun.net.httpserver.HttpServer;
import com.github.rrin.implementation.db.AuthService;
import com.github.rrin.implementation.db.WarehouseService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.SQLException;

public class WarehouseHTTPServer {

    private final HttpServer server;
    private final AuthService authService;
    private final WarehouseService warehouseService;

    public WarehouseHTTPServer(int port) throws IOException, SQLException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.authService = new AuthService();
        this.warehouseService = new WarehouseService();

        setupRoutes();
    }

    public WarehouseHTTPServer(int port, MySQLOptions options) throws IOException, SQLException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.authService = new AuthService(options);
        this.warehouseService = new WarehouseService(options);

        setupRoutes();
    }

    private void setupRoutes() {
        server.createContext("/register", new RegisterHandler(authService));
        server.createContext("/login", new LoginHandler(authService));
        server.createContext("/api/good", new GoodsHandler(authService, warehouseService));
    }

    public void start() {
        server.setExecutor(null);
        server.start();
        System.out.println("Server started on port " + server.getAddress().getPort());
    }

    public void stop() {
        server.stop(0);
        try {
            authService.close();
            warehouseService.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            WarehouseHTTPServer server = new WarehouseHTTPServer(8080);
            server.start();

            Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}