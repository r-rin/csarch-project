package com.github.rrin;

import com.github.rrin.implementation.db.MySQLManager;
import com.github.rrin.interfaces.IDatabaseManager;
import com.github.rrin.util.MySQLOptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class MySQLManagerTest {

    private static final MySQLContainer<?> MY_SQL_CONTAINER = new MySQLContainer<>();

    private IDatabaseManager databaseManager;

    @BeforeAll
    public static void startMySQLContainer() {
        MY_SQL_CONTAINER.start();
    }

    @AfterAll
    public static void stopMySQLContainer() {
        MY_SQL_CONTAINER.stop();
    }

    @BeforeEach
    public void setUp() throws SQLException, ClassNotFoundException {
        MySQLOptions options = new MySQLOptions(
                MY_SQL_CONTAINER.getJdbcUrl(),
                MY_SQL_CONTAINER.getUsername(),
                MY_SQL_CONTAINER.getPassword()
        );
        databaseManager = new MySQLManager(options);
        databaseManager.update("CREATE TABLE IF NOT EXISTS test_table (id INT PRIMARY KEY, name VARCHAR(255))");
        databaseManager.update("DELETE FROM test_table");
    }

    @Test
    public void establishConnection() throws SQLException {
        assertFalse(databaseManager.getConnection().isClosed(), "Connection should be open");
    }

    @Test
    public void testQuery() throws SQLException {
        databaseManager.update("INSERT INTO test_table (id, name) VALUES (1, 'Alice')");

        ResultSet rs = databaseManager.query("SELECT * FROM test_table WHERE id = 1");
        assertTrue(rs.next());
        assertEquals("Alice", rs.getString("name"));
        rs.close();
    }

    @Test
    public void testUpdate() throws SQLException {
        int affectedRows = databaseManager.update("INSERT INTO test_table (id, name) VALUES (2, 'Bob')");
        assertEquals(1, affectedRows);
    }

    @Test
    public void testPreparedQuery() throws SQLException {
        databaseManager.update("INSERT INTO test_table (id, name) VALUES (3, 'Charlie')");

        ResultSet rs = databaseManager.query("SELECT name FROM test_table WHERE id = ?", 3);

        assertTrue(rs.next());
        assertEquals("Charlie", rs.getString("name"));
    }

    @Test
    public void testPreparedUpdate() throws SQLException {
        int rows = databaseManager.update("INSERT INTO test_table (id, name) VALUES (?, ?)", 4, "Dana");

        assertEquals(1, rows);

        ResultSet rs = databaseManager.query("SELECT name FROM test_table WHERE id = 4");
        assertTrue(rs.next());
        assertEquals("Dana", rs.getString("name"));

    }
}
