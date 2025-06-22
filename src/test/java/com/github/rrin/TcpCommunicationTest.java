package com.github.rrin;

import com.github.rrin.dto.CommandResponse;
import com.github.rrin.util.data.DataPacket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

//TODO: Pass MySQLOptions somehow
@Disabled
public class TcpCommunicationTest {

    private static final String SERVER_HOST = "127.0.0.1";
    private static final int TCP_PORT = 5556;
    private static final int TEST_TIMEOUT = 10;

    private StoreClientTCP tcpClient;
    private StoreServerTCP tcpServer;

    @BeforeEach
    void setUp() throws Exception {
        tcpServer = new StoreServerTCP(TCP_PORT);
        tcpClient = new StoreClientTCP(SERVER_HOST, TCP_PORT);

        tcpServer.start();
        Thread.sleep(1000);
        tcpClient.start();
        tcpClient.clearDatabase();
        Thread.sleep(1000);
    }

    @AfterEach
    void tearDown() throws Exception {
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
        DataPacket<CommandResponse> response = tcpClient.createProduct("TestProduct", 10, 10, TEST_TIMEOUT);

        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertNotNull(response.getBody().getData(), "Response data should not be null");

        CommandResponse commandResponse = response.getBody().getData();
        assert(commandResponse.title().equals("Created"));
        assert(commandResponse.message().equals("Created product: TestProduct (ID: 1)"));

        response = tcpClient.updateProduct(1, null, null, 40, TEST_TIMEOUT);
        commandResponse = response.getBody().getData();
        assert(commandResponse.title().equals("Success"));
        assert(commandResponse.message().equals("Updated product (ID: 1): quantity=40 \n - ID: 1, Name: TestProduct, Value: 10.0, Quantity: 40\n"));
    }
}
