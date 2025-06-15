package com.github.rrin.util.data;

public class RawData {
    public final byte[] data;
    public final Long connectionId;

    public RawData(byte[] data, Long connectionId) {
        this.data = data;
        this.connectionId = connectionId;
    }
}
