package com.github.rrin;

import com.github.rrin.dto.CreateProduct;
import com.github.rrin.util.CommandType;
import com.github.rrin.util.Converter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @Test
    void decodeFromHexString() {
        CreateProduct expectedProduct = new CreateProduct("SomeUnknownProduct", 9125.356);
        DataPacket<CreateProduct> expectedPacket = new DataPacket<>(
                (byte) 0x13,
                (byte) 7,
                1,
                CommandType.QUERY_QUANTITY,
                5,
                expectedProduct
        );
        byte[] bytes = {19, 7, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 72, 98, 36, 0, 0, 0, 100, 0, 0, 0, 5, 17, 4, 33, 44, -99, -71, 85, -80, 64, 0, -91, -7, -72, 62, -28, -108, 10, -77, -4, 3, 10, 10, 36, -54, -111, -44, 66, -20, 93, 43, 119, 60, -87, 65, -6, 39, -27, 24, -56, -34, 67, -52, 120, 122, -74, 47, -66, 37, 53, 90, 93, 63, -125, -68, -43, 106, -95, -88, 37, 66, -90, -25, 47, 111, -123, 41};
        DataPacket<CreateProduct> result = DataPacket.fromByteArray(bytes, CreateProduct.class);
        assertEquals(expectedPacket, result);
        assertEquals(expectedProduct, result.getBody().getData());
    }

    @Test
    void decodeFromByteArray() {
        byte[] encoded = {19, 7, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 72, 98, 36, 0, 0, 0, 100, 0, 0, 0, 5, 18, 94, -80, 64, -28, 78, 15, -48, 104, -100, 93, -32, -6, -29, -34, -29, -57, 65, 109, 63, -73, -55, 85, -78, -118, -99, 0, 104, 119, 104, 81, -30, 72, 44, -124, 107, 102, -46, -25, 118, 103, -72, 10, -26, -80, 80, -59, -60, -98, 79, -35, 9, -16, 68, 94, 32, -124, -79, -107, 23, 34, -22, 71, -74, 108, -86};
        CreateProduct expected = new CreateProduct("SomeUnknownProduct", 9125.356);
        DataPacket<CreateProduct> decodedRes = DataPacket.fromByteArray(encoded, CreateProduct.class);
        assertEquals(expected, decodedRes.getBody().getData());
    }

    @Test
    void decodeWrongMagicNumber() {
        byte[] encoded = {23, 1, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 41, -120, -83, 0, 0, 0, 3, 0, 0, 0, 4, 123, 34, 110, 97, 109, 101, 34, 58, 34, 80, 114, 111, 100, 117, 99, 116, 34, 44, 34, 112, 114, 105, 99, 101, 34, 58, 49, 48, 48, 48, 46, 48, 125, 17, -85};
        assertThrows(IllegalArgumentException.class, () -> DataPacket.fromByteArray(encoded, CreateProduct.class));
    }

    @Test
    void sameProductDifferentByteArray() {
        CreateProduct p1 = new CreateProduct("SomeUnknownProduct", 9125.356);
        CreateProduct p2 = new CreateProduct("SomeUnknownProduct", 9125.356);

        DataPacket<CreateProduct> dataPacket1 = new DataPacket<>(
                (byte) 0x13,
                (byte) 2,
                6,
                CommandType.QUERY_QUANTITY,
                2,
                p1
        );
        DataPacket<CreateProduct> dataPacket2 = new DataPacket<>(
                (byte) 0x13,
                (byte) 2,
                6,
                CommandType.QUERY_QUANTITY,
                2,
                p2
        );

        String encodedP1 = Converter.bytesToHex(dataPacket1.toByteArray());
        String encodedP2 = Converter.bytesToHex(dataPacket2.toByteArray());
        assertNotEquals(encodedP1, encodedP2);
    }

}