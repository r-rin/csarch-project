package com.github.rrin.dto;

public record Group(int id, String name, String description) {
    @Override
    public String toString() {
        return " - ID: " + id +
                ", Name: " + name +
                "\n";
    }
}
