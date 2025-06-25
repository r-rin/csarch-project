package com.github.rrin.implementation.db;

import com.github.rrin.implementation.security.User;
import com.github.rrin.interfaces.IDatabaseManager;
import com.github.rrin.implementation.security.JWTTokenizer;
import com.github.rrin.util.MySQLOptions;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class AuthService implements Closeable {

    IDatabaseManager databaseManager;

    public AuthService() throws SQLException {
        MySQLOptions options = new MySQLOptions(
                "jdbc:mysql://localhost:3306/products_warehouse",
                "warehouse_user",
                "warehouse_pass"
        );
        try {
            databaseManager = new MySQLManager(options);
            init();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public AuthService(MySQLOptions options) throws SQLException {
        try {
            databaseManager = new MySQLManager(options);
            init();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void init() throws SQLException {
        String createUsersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(255) UNIQUE NOT NULL,
                    password_hash VARCHAR(255) NOT NULL
                );
                """;

        databaseManager.update(createUsersTable);
        System.out.println("Auth database initialized.");
    }

    public void clearDB() {
        try {
            databaseManager.update("DELETE FROM users");
            databaseManager.update("ALTER TABLE users AUTO_INCREMENT = 1");
            System.out.println("Auth database cleared.");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to clear auth database", e);
        }
    }

    public boolean registerUser(String username, String password) {
        if (username == null || username.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
            return false;
        }

        if (doesUserExist(username)) {
            return false;
        }

        try {
            String passwordHash = hashPassword(password);

            String insertUserSQL = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
            Connection connection = databaseManager.getConnection();

            try (PreparedStatement ps = connection.prepareStatement(insertUserSQL)) {
                ps.setString(1, username.trim());
                ps.setString(2, passwordHash);

                int affectedRows = ps.executeUpdate();
                return affectedRows > 0;
            }

        } catch (SQLException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String loginUser(String username, String password) {
        if (username == null || username.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
            return null;
        }

        try {
            User user = getUserByUsername(username.trim());
            if (user == null) {
                return null;
            }

            String hashedPassword = hashPassword(password);
            if (!hashedPassword.equals(user.getPasswordHash())) {
                return null;
            }

            return JWTTokenizer.getToken(username.trim());

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        try {
            String username = JWTTokenizer.verifyToken(token);
            return username != null && doesUserExist(username);
        } catch (Exception e) {
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        try {
            return JWTTokenizer.verifyToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean changePassword(String username, String oldPassword, String newPassword) {
        if (username == null || username.trim().isEmpty() ||
                oldPassword == null || oldPassword.trim().isEmpty() ||
                newPassword == null || newPassword.trim().isEmpty()) {
            return false;
        }

        try {
            User user = getUserByUsername(username.trim());
            if (user == null) {
                return false;
            }

            String hashedOldPassword = hashPassword(oldPassword);
            if (!hashedOldPassword.equals(user.getPasswordHash())) {
                return false;
            }

            String newPasswordHash = hashPassword(newPassword);

            String updatePasswordSQL = "UPDATE users SET password_hash = ? WHERE username = ?";

            int result = databaseManager.update(updatePasswordSQL, newPasswordHash, username.trim());
            return result > 0;

        } catch (SQLException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteUser(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }

        if (!doesUserExist(username.trim())) {
            return false;
        }

        try {
            String deleteUserSQL = "DELETE FROM users WHERE username = ?";
            int result = databaseManager.update(deleteUserSQL, username.trim());
            return result > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private User getUserByUsername(String username) {
        String query = "SELECT * FROM users WHERE username = ?";

        try (ResultSet rs = databaseManager.query(query, username)) {
            if (rs.next()) {
                int id = rs.getInt("id");
                String retrievedUsername = rs.getString("username");
                String passwordHash = rs.getString("password_hash");

                return new User(id, retrievedUsername, passwordHash);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    private boolean doesUserExist(String username) {
        String query = "SELECT COUNT(*) FROM users WHERE username = ?";

        try (ResultSet rs = databaseManager.query(query, username)) {
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    private String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashedPassword = md.digest(password.getBytes());
        return Base64.getEncoder().encodeToString(hashedPassword);
    }

    @Override
    public void close() throws IOException {
        databaseManager.close();
    }
}