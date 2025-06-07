package com.github.rrin.dto;

public record CommandResponse(int statusCode, String title, String message) {
}
