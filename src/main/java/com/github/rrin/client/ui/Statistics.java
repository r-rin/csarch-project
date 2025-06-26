package com.github.rrin.client.ui;

import com.github.rrin.dto.Product;

import java.util.List;

public class Statistics {
    public static double calculateTotalValue(List<Product> products) {
        double totalValue = 0;
        for (Product product : products) {
            totalValue += product.price() * product.quantity();
        }
        return totalValue;
    }
}
