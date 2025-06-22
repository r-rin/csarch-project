package com.github.rrin.implementation.db;

import com.github.rrin.interfaces.IDatabaseManager;
import com.github.rrin.util.MySQLOptions;

import java.sql.*;

public class MySQLManager implements IDatabaseManager {

    Connection connection;

    public MySQLManager(MySQLOptions options) {
        connect(options);
    }

    private void connect(MySQLOptions options) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.connection = DriverManager.getConnection(options.url(), options.user(), options.password());
            System.out.println("MySQL connection established");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found");
        } catch (SQLException e) {
            System.err.println("MySQL connection failed");
        }
    }

    @Override
    public ResultSet query(String sql) throws SQLException {
        Statement statement = connection.createStatement();
        return statement.executeQuery(sql);
    }

    @Override
    public ResultSet query(String preparedString, Object... objects) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(preparedString);
        for (int i = 0; i < objects.length; i++) {
            preparedStatement.setObject(i + 1, objects[i]);
        }

        return preparedStatement.executeQuery();
    }

    @Override
    public int update(String sql) throws SQLException {
        Statement statement = connection.createStatement();
        return statement.executeUpdate(sql);
    }

    @Override
    public int update(String preparedString, Object... objects) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(preparedString);
        for (int i = 0; i < objects.length; i++) {
            preparedStatement.setObject(i + 1, objects[i]);
        }

        return preparedStatement.executeUpdate();
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("MySQL connection closed");
            }
        } catch (SQLException e) {
            System.err.println("Error at MySQL connection close: " + e.getMessage());
        }
    }

    @Override
    public Connection getConnection() {
        return connection;
    }
}
