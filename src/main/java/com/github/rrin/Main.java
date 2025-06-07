package com.github.rrin;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        CreateProduct createProduct = new CreateProduct("Product", 1000.0);
        DataPacket<CreateProduct> dataPacket = new DataPacket<CreateProduct>(
                (byte) 0x13,
                (byte) 7,
                1,
                CommandType.QUERY_QUANTITY,
                5,
                createProduct
        );

        byte[] encoded = dataPacket.toByteArray();
        System.out.printf("Encoded data: %s%n", Arrays.toString(encoded));
        System.out.println("Hex encoded data: " + bytesToHex(encoded));
        DataPacket<CreateProduct> decoded = DataPacket.fromByteArray(encoded, CreateProduct.class);
        System.out.println("Decoded data: " + decoded.getBody().getData());
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