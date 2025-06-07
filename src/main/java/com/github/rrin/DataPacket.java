/*
Packet structure:

Offset  Length  Mnemonic    Notes
00      1	    bMagic	    Байт, що вказує на початок пакету - значення 13h (h - значить hex)
01	    1	    bSrc	    Унікальний номер клієнтського застосування
02	    8	    bPktId	    Номер повідомлення. Номер постійно збільшується. В форматі big-endian
10	    4	    wLen	    Довжина пакету даних big-endian
14	    2	    wCrc16	    CRC16 байтів (00-13) big-endian
16	    wLen	bMsq	    Message - корисне повідомлення
16+wLen	2	    wCrc16	    CRC16 байтів (16 до 16+wLen-1) big-endian

Структура повідомлення (message)
Offset	Length	Mnemonic 	Notes
00	    4	    cType	    Код команди big-endian
04	    4	    bUserId     Від кого надіслане повідомлення. В системі може бути багато клієнтів. А на кожному з цих клієнтів може працювати один з багатьох працівників. big-endian
08	    wLen-8	message     корисна інформація, можна покласти JSON як масив байтів big-endian
*/

package com.github.rrin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rrin.util.CRC16;
import com.github.rrin.util.CommandType;
import com.github.rrin.util.DataEncryption;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;

public class DataPacket<T> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final byte magicByte;
    private final byte sourceId;
    private final long packetId;
    private final int bodyLength;
    private final short headerChecksum;
    private final PacketBody<T> body;
    private final short bodyChecksum;

    public DataPacket(byte magicByte, byte sourceId, long packetId, CommandType command, int userId, T data) {
        this.body = new PacketBody<>(command, userId, data);

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
            message = DataEncryption.decrypt(message);
            T dataObject = objectMapper.readValue(message, dataClass);
            return new DataPacket<>(magicByte, sourceId, packetId, CommandType.fromCode(commandType), userId, dataObject);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] toByteArray() {

        int msgSize = getBodyLength();
        int packetSize = 1 + 1 + 8 + 4 + 2 + msgSize + 2;

        ByteBuffer buffer = ByteBuffer.allocate(packetSize).order(ByteOrder.BIG_ENDIAN);

        ByteBuffer headerBuffer = ByteBuffer.allocate(1 + 1 + 8 + 4).order(ByteOrder.BIG_ENDIAN);
        headerBuffer.put(getMagicByte())        // bMagic
                .put(getSourceId())             // bSrc
                .putLong(getPacketId())         // bPktId
                .putInt(msgSize);               // wLen

        PacketBody<T> body = getBody();
        byte[] bodyData = body.toByteArray();

        buffer.put(headerBuffer.array())    // packet head
                .putShort(CRC16.sum(headerBuffer.array()))      // packet head checksum
                .put(bodyData)    // packet body
                .putShort(CRC16.sum(bodyData));       // packet body checksum

        return buffer.array();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DataPacket<?> that = (DataPacket<?>) o;
        return magicByte == that.magicByte && sourceId == that.sourceId && packetId == that.packetId && bodyLength == that.bodyLength && headerChecksum == that.headerChecksum && Objects.equals(body, that.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(magicByte, sourceId, packetId, bodyLength, headerChecksum, body, bodyChecksum);
    }

    public static class PacketBody<T> {
        private final CommandType command;
        private final int userId;
        private final T data;

        PacketBody(CommandType command, int userId, T data) {
            this.command = command;
            this.userId = userId;
            this.data = data;
        }

        public CommandType getCommand() {
            return command;
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
                byte[] bytes = objectMapper.writeValueAsBytes(data);
                return DataEncryption.encrypt(bytes);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        public byte[] toByteArray() {
            byte[] dataBytes = getDataInBytes();
            ByteBuffer buffer = ByteBuffer.allocate(calcBodyLength()).order(ByteOrder.BIG_ENDIAN);
            buffer.putInt(command.getCode())
                    .putInt(userId)
                    .put(dataBytes);
            return buffer.array();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            PacketBody<?> that = (PacketBody<?>) o;
            return command == that.command && userId == that.userId && Objects.equals(data, that.data);
        }

        @Override
        public int hashCode() {
            return Objects.hash(command, userId, data);
        }
    }
}
