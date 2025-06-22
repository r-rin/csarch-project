package com.github.rrin;

import com.github.rrin.dto.CommandResponse;
import com.github.rrin.util.data.DataPacket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

//TODO: Pass MySQLOptions somehow
@Disabled
public class UdpCommunicationTest {
    private static final String SERVER_HOST = "127.0.0.1";
    private static final int UDP_PORT = 5556;
    private static final int TEST_TIMEOUT = 10;

    private StoreClientUDP udpClient;
    private StoreServerUDP udpServer;

    @BeforeEach
    void setUp() throws Exception {
        udpServer = new StoreServerUDP(UDP_PORT);
        udpClient = new StoreClientUDP(SERVER_HOST, UDP_PORT);

        udpServer.start();
        Thread.sleep(1000);
        udpClient.start();
        udpClient.clearDatabase();
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
        DataPacket<CommandResponse> response = udpClient.createProduct("TestProduct", 10, 10, TEST_TIMEOUT);

        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertNotNull(response.getBody().getData(), "Response data should not be null");

        CommandResponse commandResponse = response.getBody().getData();
        assert(commandResponse.title().equals("Created"));
        assert(commandResponse.message().equals("Created product: TestProduct (ID: 1)"));

        response = udpClient.updateProduct(1, null, null, 40, TEST_TIMEOUT);
        commandResponse = response.getBody().getData();
        assert(commandResponse.title().equals("Success"));
        assert(commandResponse.message().equals("Updated product (ID: 1): quantity=40 \n - ID: 1, Name: TestProduct, Value: 10.0, Quantity: 40\n"));
    }
}
