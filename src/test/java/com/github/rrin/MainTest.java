package com.github.rrin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @Test
    void encodeToHexString() {
        CreateProduct product = new CreateProduct("SomeUnknownProduct", 9125.356);
        String hexRes = Main.bytesToHex(Main.encode(product));
        String expected = "130100000000000000020000003684E900000003000000047B226E616D65223A22536F6D65556E6B6E6F776E50726F64756374222C227072696365223A393132352E3335367D8950";
        assertEquals(expected, hexRes);
    }

    @Test
    void decodeFromByteArray() {
        byte[] encoded = {19, 1, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 54, -124, -23, 0, 0, 0, 3, 0, 0, 0, 4, 123, 34, 110, 97, 109, 101, 34, 58, 34, 83, 111, 109, 101, 85, 110, 107, 110, 111, 119, 110, 80, 114, 111, 100, 117, 99, 116, 34, 44, 34, 112, 114, 105, 99, 101, 34, 58, 57, 49, 50, 53, 46, 51, 53, 54, 125, -119, 80};
        CreateProduct expected = new CreateProduct("SomeUnknownProduct", 9125.356);
        CreateProduct decodedRes = Main.decode(encoded);
        assertEquals(expected, decodedRes);
    }

    @Test
    void decodeWrongMagicNumber() {
        byte[] encoded = {23, 1, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 41, -120, -83, 0, 0, 0, 3, 0, 0, 0, 4, 123, 34, 110, 97, 109, 101, 34, 58, 34, 80, 114, 111, 100, 117, 99, 116, 34, 44, 34, 112, 114, 105, 99, 101, 34, 58, 49, 48, 48, 48, 46, 48, 125, 17, -85};
        assertThrows(IllegalArgumentException.class, () -> Main.decode(encoded));
    }

    @Test
    void sameProductSameByteArray() {
        CreateProduct p1 = new CreateProduct("SomeUnknownProduct", 9125.356);
        CreateProduct p2 = new CreateProduct("SomeUnknownProduct", 9125.356);
        byte[] encodedP1 = Main.encode(p1);
        byte[] encodedP2 = Main.encode(p2);
        assertArrayEquals(encodedP1, encodedP2);
    }

}