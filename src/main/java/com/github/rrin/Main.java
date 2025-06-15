package com.github.rrin;

public class Main {
    public static void main(String[] args) {
        String serverHost = "127.0.0.1";
        int serverPort = 5555;

        StoreServerUDP app = new StoreServerUDP(serverPort);
        StoreClientUDP client = new StoreClientUDP(serverHost, serverPort);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n!!! Stopping everything !!!");
            app.stop();
            client.stop();
        }));

        try {
            app.start();
            client.start();

            client.addGoods("Laptop", 15);
            client.setPrice("Laptop", 1199.9);
            client.queryQuantity("Laptop");

            while (client.isRunning()) {
                Thread.sleep(1000);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            client.stop();
        }
    }
}