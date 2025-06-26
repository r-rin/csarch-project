package com.github.rrin.dto;

public record Product(int id, String name, String manufacturer, String description, double price, int quantity) {
    public String toFormatted() {
        return " - ID: " + id +
                ", Name: " + name +
                ", Value: " + price +
                ", Quantity: " + quantity +
                "\n";
    }

    @Override
    public String toString() {
        return name;
    }
}
