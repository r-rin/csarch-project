package com.github.rrin;

import com.github.rrin.dto.CommandResponse;
import com.github.rrin.util.data.DataPacket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UdpCommunicationTest {
    private static final String SERVER_HOST = "127.0.0.1";
    private static final int UDP_PORT = 5556;
    private static final int TEST_TIMEOUT = 10;

    private StoreClientUDP udpClient;
    private StoreServerUDP udpServer;

    @BeforeEach
    void setUp() throws InterruptedException {
        udpServer = new StoreServerUDP(UDP_PORT);
        udpClient = new StoreClientUDP(SERVER_HOST, UDP_PORT);

        udpServer.start();
        Thread.sleep(1000);
        udpClient.start();
        Thread.sleep(1000);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (udpClient != null) {
            udpClient.stop();
        }
        Thread.sleep(500);

        if (udpServer != null) {
            udpServer.stop();
        }
        Thread.sleep(500);
    }

    @Test
    public void testConnection() throws Exception {
        assertTrue(udpClient.isRunning(), "UDP client should be running");
        int code = udpClient.isServerRunning().getBody().getData().statusCode();
        assertEquals(200, code);
    }

    @Test
    void testTcpAddGoods() throws Exception {
        DataPacket<CommandResponse> response = udpClient.addGoods("TestProductUDP", 10, TEST_TIMEOUT);

        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertNotNull(response.getBody().getData(), "Response data should not be null");

        CommandResponse commandResponse = response.getBody().getData();
        assert(commandResponse.title().equals("Success!"));
        assert(commandResponse.message().equals("Added 10 of TestProductUDP. New quantity: 10"));

        response = udpClient.addGoods("TestProductUDP", 30, TEST_TIMEOUT);
        commandResponse = response.getBody().getData();
        assert(commandResponse.title().equals("Success!"));
        assert(commandResponse.message().equals("Added 30 of TestProductUDP. New quantity: 40"));
    }
}
