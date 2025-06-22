package com.github.rrin.implementation.db;

import com.github.rrin.interfaces.IDatabaseManager;
import com.github.rrin.util.MySQLOptions;

import java.sql.*;

public class MySQLManager implements IDatabaseManager {

    MySQLOptions options;

    public MySQLManager(MySQLOptions options) {
        this.options = options;
        connect(options);
    }

    private void connect(MySQLOptions options) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found");
        }
    }

    @Override
    public ResultSet query(String sql) throws SQLException {
        Statement statement = getConnection().createStatement();
        return statement.executeQuery(sql);
    }

    @Override
    public ResultSet query(String preparedString, Object... objects) throws SQLException {
        PreparedStatement preparedStatement = getConnection().prepareStatement(preparedString);
        for (int i = 0; i < objects.length; i++) {
            preparedStatement.setObject(i + 1, objects[i]);
        }

        return preparedStatement.executeQuery();
    }

    @Override
    public int update(String sql) throws SQLException {
        Statement statement = getConnection().createStatement();
        return statement.executeUpdate(sql);
    }

    @Override
    public int update(String preparedString, Object... objects) throws SQLException {
        PreparedStatement preparedStatement = getConnection().prepareStatement(preparedString);
        for (int i = 0; i < objects.length; i++) {
            preparedStatement.setObject(i + 1, objects[i]);
        }

        return preparedStatement.executeUpdate();
    }

    @Override
    public Connection getConnection() {
        try {
            Connection connection = DriverManager.getConnection(options.url(), options.user(), options.password());
            return connection;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
