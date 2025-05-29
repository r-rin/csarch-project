package com.github.rrin;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        String content = "Hello World!";
        byte[] encoded = encode(content);
        System.out.println(bytesToHex(encoded));
        String decoded = decode(encoded);
        System.out.println(decoded);
    }

    public static byte[] encode(String content) {
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        int msgSize = contentBytes.length + 4 + 4;
        int packetSize = 1 + 1 + 8 + 4 + 2 + msgSize + 2;

        ByteBuffer buffer = ByteBuffer.allocate(packetSize).order(ByteOrder.BIG_ENDIAN);

        ByteBuffer headerBuffer = ByteBuffer.allocate(1 + 1 + 8 + 4).order(ByteOrder.BIG_ENDIAN);
        headerBuffer.put((byte) 0x13)       // bMagic
                .put((byte) 1)              // bSrc
                .putLong(2)           // bPktId
                .putInt(msgSize);           // wLen

        ByteBuffer bodyBuffer = ByteBuffer.allocate(msgSize).order(ByteOrder.BIG_ENDIAN);
        bodyBuffer.putInt(3)          // cType
                .putInt(4)            // bUserId
                .put(contentBytes);         // message

        buffer.put(headerBuffer.array())    // packet head
                .putShort(CRC16.sum(headerBuffer.array()))      // packet head checksum
                .put(bodyBuffer.array())    // packet body
                .putShort(CRC16.sum(bodyBuffer.array()));       // packet body checksum

        return buffer.array();
    }

    public static String decode(byte[] content) {
        ByteBuffer buffer = ByteBuffer.wrap(content);
        byte bMagic = buffer.get();
        if (bMagic != 0x13) { throw new IllegalArgumentException(); }
        byte bSrc = buffer.get();
        long bPktId = buffer.getLong();
        int wLen = buffer.getInt();
        short wHeadSum =  buffer.getShort();
        short expectedHeadSum = CRC16.sum(Arrays.copyOfRange(content, 0, 14));
        if (wHeadSum != expectedHeadSum) { throw new IllegalArgumentException(); }
        int cType = buffer.getInt();
        int bUserId = buffer.getInt();
        byte[] message = new byte[wLen - 8];
        buffer.get(message, 0, message.length);
        short wBodySum = buffer.getShort();
        short expectedBodySum = CRC16.sum(Arrays.copyOfRange(content, 16, 16 + wLen));
        if (wBodySum != expectedBodySum) { throw new IllegalArgumentException(); }

        return new String(message, StandardCharsets.UTF_8);
    }

    public static String bytesToHex(byte[] bytes) {
        final byte[] hexArray = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }
}

/*
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