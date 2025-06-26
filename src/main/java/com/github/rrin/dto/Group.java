package com.github.rrin.dto;

public record Group(int id, String name, String description) {
    public String toFormatted() {
        return " - ID: " + id +
                ", Name: " + name +
                "\n";
    }

    @Override
    public String toString() {
        return name;
    }
}
