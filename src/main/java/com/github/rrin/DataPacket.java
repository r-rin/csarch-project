package com.github.rrin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class DataPacket<T> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final byte magicByte;
    private final byte sourceId;
    private final long packetId;
    private final int bodyLength;
    private final short headerChecksum;
    private final PacketBody<T> body;
    private final short bodyChecksum;

    DataPacket(byte magicByte, byte sourceId, long packetId, int commandType, int userId, T data) {
        this.body = new PacketBody<>(commandType, userId, data);

        this.magicByte = magicByte;
        this.sourceId = sourceId;
        this.packetId = packetId;
        this.bodyLength = body.calcBodyLength();

        ByteBuffer headerBuffer = ByteBuffer.allocate(1 + 1 + 8 + 4).order(ByteOrder.BIG_ENDIAN);
        headerBuffer.put(magicByte)
                .put(sourceId)
                .putLong(packetId)
                .putInt(bodyLength);

        this.headerChecksum = CRC16.sum(headerBuffer.array());
        this.bodyChecksum = CRC16.sum(body.toByteArray());
    }

    public byte getMagicByte() {
        return magicByte;
    }

    public byte getSourceId() {
        return sourceId;
    }

    public long getPacketId() {
        return packetId;
    }

    public int getBodyLength() {
        return bodyLength;
    }

    public short getHeaderChecksum() {
        return headerChecksum;
    }

    public PacketBody<T> getBody() {
        return body;
    }

    public short getBodyChecksum() {
        return bodyChecksum;
    }

    public static <T> DataPacket<T> fromByteArray(byte[] data, Class<T> dataClass) {
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);

        byte magicByte = buffer.get();
        if (magicByte != (byte) 0x13) { throw new IllegalArgumentException(); };

        byte sourceId = buffer.get();
        long packetId = buffer.getLong();
        int bodyLength = buffer.getInt();

        short headerChecksum =  buffer.getShort();
        short expectedHeadSum = CRC16.sum(Arrays.copyOfRange(data, 0, 14));
        if (headerChecksum != expectedHeadSum) { throw new IllegalArgumentException(); }

        int commandType = buffer.getInt();
        int userId = buffer.getInt();

        byte[] message = new byte[bodyLength - 8];
        buffer.get(message, 0, message.length);

        short wBodySum = buffer.getShort();
        short expectedBodySum = CRC16.sum(Arrays.copyOfRange(data, 16, 16 + bodyLength));
        if (wBodySum != expectedBodySum) { throw new IllegalArgumentException(); }

        try {
            T dataObject = objectMapper.readValue(message, dataClass);
            return new DataPacket<>(magicByte, sourceId, packetId, commandType, userId, dataObject);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] toByteArray() {
        byte[] contentBytes = null;
        try {
            contentBytes = objectMapper.writeValueAsBytes(getBody().getData());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        int msgSize = getBodyLength();
        int packetSize = 1 + 1 + 8 + 4 + 2 + msgSize + 2;

        ByteBuffer buffer = ByteBuffer.allocate(packetSize).order(ByteOrder.BIG_ENDIAN);

        ByteBuffer headerBuffer = ByteBuffer.allocate(1 + 1 + 8 + 4).order(ByteOrder.BIG_ENDIAN);
        headerBuffer.put(getMagicByte())        // bMagic
                .put(getSourceId())             // bSrc
                .putLong(getPacketId())         // bPktId
                .putInt(msgSize);               // wLen

        PacketBody<T> body = getBody();
        ByteBuffer bodyBuffer = ByteBuffer.allocate(msgSize).order(ByteOrder.BIG_ENDIAN);
        bodyBuffer.putInt(body.getCommandType())          // cType
                .putInt(body.getUserId())                 // bUserId
                .put(contentBytes);                       // message

        buffer.put(headerBuffer.array())    // packet head
                .putShort(CRC16.sum(headerBuffer.array()))      // packet head checksum
                .put(bodyBuffer.array())    // packet body
                .putShort(CRC16.sum(bodyBuffer.array()));       // packet body checksum

        return buffer.array();
    }

    public static class PacketBody<T> {
        private final int commandType;
        private final int userId;
        private final T data;

        PacketBody(int commandType, int userId, T data) {
            this.commandType = commandType;
            this.userId = userId;
            this.data = data;
        }

        public int getCommandType() {
            return commandType;
        }

        public int getUserId() {
            return userId;
        }

        public T getData() {
            return data;
        }

        private int calcBodyLength() {
            return getDataInBytes().length + 4 + 4;
        }

        private byte[] getDataInBytes() {
            try {
                return objectMapper.writeValueAsBytes(data);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        public byte[] toByteArray() {
            byte[] dataBytes = getDataInBytes();
            ByteBuffer buffer = ByteBuffer.allocate(calcBodyLength()).order(ByteOrder.BIG_ENDIAN);
            buffer.putInt(commandType)
                    .putInt(userId)
                    .put(dataBytes);
            return buffer.array();
        }
    }
}
