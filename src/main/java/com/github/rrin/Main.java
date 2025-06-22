package com.github.rrin;

import com.github.rrin.implementation.db.MySQLManager;
import com.github.rrin.util.MySQLOptions;

public class Main {
    public static void main(String[] args) {
        MySQLOptions options = new MySQLOptions(
                "jdbc:mysql://localhost:3306/products_warehouse",
                "warehouse_user",
                "warehouse_pass"
        );

        MySQLManager databaseManager = new MySQLManager(options);
    }
}