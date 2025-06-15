package com.github.rrin;

import com.github.rrin.util.MockClientUdp;

public class Main {
    public static void main(String[] args) {
        String serverHost = "127.0.0.1";
        int serverPort = 5555;

        AppPipeline app = new AppPipeline(serverPort);
        MockClientUdp client = new MockClientUdp(serverHost, serverPort);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n!!! Stopping test client !!!");
            app.stop();
            client.stop();
        }));

        try {
            app.start();
            client.start();

            while (client.isRunning()) {
                Thread.sleep(1000);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            client.stop();
        }
    }
}