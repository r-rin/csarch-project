package com.github.rrin.util.data;

public class RequestData {
    private byte sourceId;
    private long packetId;
    private int userId;
    private Object response;
    private final long connectionId;

    public RequestData(byte sourceId, long packetId, int userId, Object response, long connectionId) {
        this.sourceId = sourceId;
        this.packetId = packetId;
        this.userId = userId;
        this.response = response;
        this.connectionId = connectionId;
    }

    public long getConnectionId() {
        return connectionId;
    }

    public byte getSourceId() {
        return sourceId;
    }

    public void setSourceId(byte sourceId) {
        this.sourceId = sourceId;
    }

    public long getPacketId() {
        return packetId;
    }

    public void setPacketId(long packetId) {
        this.packetId = packetId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Object getResponse() {
        return response;
    }

    public void setResponse(Object response) {
        this.response = response;
    }
}