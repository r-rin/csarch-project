package com.github.rrin.interfaces;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface IDatabaseManager extends Closeable {
    ResultSet query(String sql) throws SQLException;
    ResultSet query(String sql, Object... objects) throws SQLException;
    int update(String sql) throws SQLException;
    int update(String sql, Object... objects) throws SQLException;
    Connection getConnection();
}
