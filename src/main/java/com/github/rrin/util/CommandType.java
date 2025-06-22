package com.github.rrin.util;

public enum CommandType {
    // Product operations
    CREATE_PRODUCT(1),
    GET_PRODUCT(2),
    UPDATE_PRODUCT(3),
    DELETE_PRODUCT(4),
    SEARCH_PRODUCTS(5),
    GET_ALL_PRODUCTS(6),

    // Group operations
    CREATE_GROUP(7),
    GET_GROUP(8),
    UPDATE_GROUP(9),
    DELETE_GROUP(10),
    GET_ALL_GROUPS(11),

    // Product-Group relationship operations
    ADD_PRODUCT_TO_GROUP(12),
    REMOVE_PRODUCT_FROM_GROUP(13),
    GET_PRODUCT_GROUPS(14),
    GET_GROUP_PRODUCTS(15),

    // System operations
    IS_RUNNING(21),
    RESPONSE(22),
    CLEAR_DB(23);

    private final int code;

    CommandType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static CommandType fromCode(int code) {
        for (CommandType c : values()) {
            if (c.code == code) return c;
        }
        throw new IllegalArgumentException("Invalid command code: " + code);
    }
}
