package com.github.rrin.util;

import com.github.rrin.dto.CommandResponse;
import com.github.rrin.util.data.DataPacket;

import java.util.concurrent.CompletableFuture;

public record DelayedRequest(CommandType command, Object data, CompletableFuture<DataPacket<CommandResponse>> future, long packetId) { }
