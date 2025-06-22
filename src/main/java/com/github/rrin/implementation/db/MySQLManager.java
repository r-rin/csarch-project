package com.github.rrin.implementation.db;

import com.github.rrin.interfaces.IDatabaseManager;
import com.github.rrin.util.MySQLOptions;

import java.io.IOException;
import java.sql.*;

public class MySQLManager implements IDatabaseManager {

    Connection connection;
    MySQLOptions options;

    public MySQLManager(MySQLOptions options) throws SQLException, ClassNotFoundException {
        this.options = options;
        connect(options);
    }

    private void connect(MySQLOptions options) throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        this.connection = DriverManager.getConnection(options.url(), options.user(), options.password());
    }

    @Override
    public ResultSet query(String sql) throws SQLException {
        Statement statement = this.connection.createStatement();
        return statement.executeQuery(sql);
    }

    @Override
    public ResultSet query(String preparedString, Object... objects) throws SQLException {
        PreparedStatement preparedStatement = this.connection.prepareStatement(preparedString);
        for (int i = 0; i < objects.length; i++) {
            preparedStatement.setObject(i + 1, objects[i]);
        }

        return preparedStatement.executeQuery();
    }

    @Override
    public int update(String sql) throws SQLException {
        Statement statement = this.connection.createStatement();
        return statement.executeUpdate(sql);
    }

    @Override
    public int update(String preparedString, Object... objects) throws SQLException {
        PreparedStatement preparedStatement = this.connection.prepareStatement(preparedString);
        for (int i = 0; i < objects.length; i++) {
            preparedStatement.setObject(i + 1, objects[i]);
        }

        return preparedStatement.executeUpdate();
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public void close() throws IOException {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
