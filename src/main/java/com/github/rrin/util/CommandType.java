package com.github.rrin.util;

public enum CommandType {
    QUERY_QUANTITY(1),
    REMOVE_GOODS(2),
    ADD_GOODS(3),
    ADD_GROUP(4),
    ADD_PRODUCT_TO_GROUP(5),
    SET_PRICE(6),
    RESPONSE(7),
    IS_RUNNING(8);

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
