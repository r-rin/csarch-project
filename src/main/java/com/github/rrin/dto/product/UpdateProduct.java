package com.github.rrin.dto.product;

public record UpdateProduct(int id, String name, String manufacturer, String description, Double price, Integer quantity) {}
