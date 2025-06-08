package com.github.rrin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rrin.dto.QueryQuantity;
import com.github.rrin.implementation.Decrypter;
import com.github.rrin.util.CommandType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class DecrypterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private BlockingQueue<byte[]> inputQueue;
    private BlockingQueue<DataPacket<?>> outputQueue;
    private Decrypter decrypter;

    @BeforeEach
    void setUp() {
        inputQueue = new ArrayBlockingQueue<>(10);
        outputQueue = new ArrayBlockingQueue<>(10);
        decrypter = new Decrypter(inputQueue, outputQueue);
    }

    @AfterEach
    void tearDown() {
        if (decrypter.isRunning()) {
            decrypter.stop();
        }
    }

    @Test
    void testStartDecrypter() {
        assertFalse(decrypter.isRunning());

        decrypter.start();

        assertTrue(decrypter.isRunning());
    }

    @Test
    void testDecrypterBytesIntoDataPacket() throws Exception {
        decrypter.start();
        assertTrue(decrypter.isRunning());
        DataPacket<QueryQuantity> packet = new DataPacket<>(
                (byte) 0x13,
                (byte) 2,
                100,
                CommandType.QUERY_QUANTITY,
                1,
                new QueryQuantity("Orange")
        );

        inputQueue.put(packet.toByteArray());

        DataPacket<QueryQuantity> decrypted = (DataPacket<QueryQuantity>) outputQueue.poll(2, TimeUnit.SECONDS);
        assertNotNull(decrypted);

        assertEquals(packet.getPacketId(), decrypted.getPacketId());
        assertEquals(packet.getSourceId(), decrypted.getSourceId());
        assertEquals(packet.getMagicByte(), decrypted.getMagicByte());
        assertEquals(packet.getSourceId(), decrypted.getSourceId());
        assertEquals(packet.getBodyLength(), decrypted.getBodyLength());
        assertEquals(packet.getHeaderChecksum(), decrypted.getHeaderChecksum());

        assertEquals(packet.getBody().getData(), objectMapper.convertValue(decrypted.getBody().getData(), QueryQuantity.class));
    }
}
