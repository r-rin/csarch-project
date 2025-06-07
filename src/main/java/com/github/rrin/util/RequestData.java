package com.github.rrin.util;

public record RequestData(byte sourceId, long packetId, int userId, Object response) {
}
