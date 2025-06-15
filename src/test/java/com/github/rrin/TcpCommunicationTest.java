package com.github.rrin;

import com.github.rrin.dto.CommandResponse;
import com.github.rrin.util.data.DataPacket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TcpCommunicationTest {

    private static final String SERVER_HOST = "127.0.0.1";
    private static final int TCP_PORT = 5556;
    private static final int TEST_TIMEOUT = 10;

    private StoreClientTCP tcpClient;
    private StoreServerTCP tcpServer;

    @BeforeEach
    void setUp() throws InterruptedException {
        tcpServer = new StoreServerTCP(TCP_PORT);
        tcpClient = new StoreClientTCP(SERVER_HOST, TCP_PORT);

        tcpServer.start();
        Thread.sleep(1000);
        tcpClient.start();
        Thread.sleep(1000);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (tcpClient != null) {
            tcpClient.stop();
        }
        Thread.sleep(500);

        if (tcpServer != null) {
            tcpServer.stop();
        }
        Thread.sleep(500);
    }

    @Test
    public void testTcpConnection() {
        assertTrue(tcpClient.isRunning(), "TCP client should be running");
        assertTrue(tcpClient.isServerAvailable(), "TCP client should be connected to server");
    }

    @Test
    void testTcpAddGoods() throws Exception {
        DataPacket<CommandResponse> response = tcpClient.addGoods("TestProduct", 10, TEST_TIMEOUT);

        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertNotNull(response.getBody().getData(), "Response data should not be null");

        CommandResponse commandResponse = response.getBody().getData();
        assert(commandResponse.title().equals("Success!"));
        assert(commandResponse.message().equals("Added 10 of TestProduct. New quantity: 10"));

        response = tcpClient.addGoods("TestProduct", 30, TEST_TIMEOUT);
        commandResponse = response.getBody().getData();
        assert(commandResponse.title().equals("Success!"));
        assert(commandResponse.message().equals("Added 30 of TestProduct. New quantity: 40"));
    }
}
