package com.github.rrin.dto.product;

public record SearchProducts(
        String name,
        Integer groupId,
        Double minPrice,
        Double maxPrice,
        Integer minQuantity,
        Integer maxQuantity,
        int page,
        int pageSize
) {}
