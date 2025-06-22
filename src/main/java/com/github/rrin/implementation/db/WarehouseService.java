package com.github.rrin.implementation.db;

import com.github.rrin.dto.Group;
import com.github.rrin.dto.Product;
import com.github.rrin.interfaces.IDatabaseManager;
import com.github.rrin.util.MySQLOptions;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WarehouseService {

    IDatabaseManager databaseManager;

    public WarehouseService() throws SQLException {
        MySQLOptions options = new MySQLOptions(
                "jdbc:mysql://localhost:3306/products_warehouse",
                "root",
                "rootpass"
        );
        databaseManager = new MySQLManager(options);
        init();
    }

    private void init() throws SQLException {
        String createProductsTable = """
                CREATE TABLE IF NOT EXISTS products (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        value DECIMAL(10,2) NOT NULL,
                        quantity INT NOT NULL
                    );
                """;

        String createGroupsTable = """
                    CREATE TABLE IF NOT EXISTS groups (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(255) UNIQUE NOT NULL
                    );
                """;

        String createGroupProductsTable = """
                    CREATE TABLE IF NOT EXISTS group_products (
                        group_id INT NOT NULL,
                        product_id INT NOT NULL,
                        PRIMARY KEY (group_id, product_id),
                        FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE,
                        FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
                    );
                """;

        databaseManager.update(createProductsTable);
        databaseManager.update(createGroupsTable);
        databaseManager.update(createGroupProductsTable);

        System.out.println("Database initialized.");
    }

    public int createProduct(String name, double price, int quantity) {
        String insertProductSQL = "INSERT INTO products (name, value, quantity) VALUES (?, ?, ?)";
        try (var connection = databaseManager.getConnection();
             var ps = connection.prepareStatement(insertProductSQL, java.sql.Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, name);
            ps.setDouble(2, price);
            ps.setInt(3, quantity);
            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating product failed, no rows affected.");
            }

            try (var rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    throw new SQLException("Creating product failed, no ID obtained.");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public int createGroup(String name) {
        String insertProductSQL = "INSERT INTO groups (name) VALUES (?)";
        try (var connection = databaseManager.getConnection();
             var ps = connection.prepareStatement(insertProductSQL, java.sql.Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, name);
            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating group failed, no rows affected.");
            }

            try (var rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    throw new SQLException("Creating group failed, no ID obtained.");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public boolean setProductQuantities(int productId, int quantity) {
        boolean productExist = doProductExist(productId);
        if (!productExist) {return false;}

        String query = "UPDATE products SET quantity = ? WHERE id = ?";

        try {
            int res = databaseManager.update(query, quantity, productId);
            if (res > 0) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean setProductName(int id, String name) {
        boolean productExist = doProductExist(id);
        if (!productExist) {return false;}

        String query = "UPDATE products SET name = ? WHERE id = ?";

        try {
            int res = databaseManager.update(query, name, id);
            if (res > 0) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean setProductPrice(int id, double price) {
        boolean productExist = doProductExist(id);
        if (!productExist) {return false;}

        String query = "UPDATE products SET value = ? WHERE id = ?";

        try {
            int res = databaseManager.update(query, price, id);
            if (res > 0) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean setGroupName(int id, String name) {
        boolean groupExist = doGroupExist(id);
        if (!groupExist) {return false;}

        String query = "UPDATE groups SET name = ? WHERE id = ?";

        try {
            int res = databaseManager.update(query, name, id);
            if (res > 0) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean addProductToGroup(int groupId, int productId) {
        boolean prodExists = doProductExist(productId);
        boolean groupExists = doGroupExist(groupId);

        if (!prodExists || !groupExists) { return false; }

        boolean isProdInGroup = isProductInGroup(groupId, productId);

        if (isProdInGroup) { return false; }

        String query = "INSERT INTO group_products (group_id, product_id) VALUES (?, ?)";

        try {
            int res = databaseManager.update(query, groupId, productId);

            if (res > 0) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean removeProductFromGroup(int groupId, int productId) {
        boolean prodExists = doProductExist(productId);
        boolean groupExists = doGroupExist(groupId);

        if (!prodExists || !groupExists) { return false; }

        boolean isProdInGroup = isProductInGroup(groupId, productId);

        if (!isProdInGroup) { return true; }

        String query = "DELETE FROM group_products WHERE group_id = ? AND product_id = ?";

        try {
            int res = databaseManager.update(query, groupId, productId);

            if (res > 0) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public Product removeProduct(int id) {
        boolean productExist = doProductExist(id);
        if (!productExist) {return null;}

        Product product = getProduct(id);
        String query = "DELETE FROM products WHERE id = ?";

        try {
            int res = databaseManager.update(query, id);

            if (res > 0) {
                return product;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public Group removeGroup(int id) {
        boolean groupExist = doGroupExist(id);
        if (!groupExist) {return null;}

        Group group = getGroup(id);
        String query = "DELETE FROM groups WHERE id = ?";

        try {
            int res = databaseManager.update(query, id);

            if (res > 0) {
                return group;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<Group> getGroups() {
        List<Group> groups = new ArrayList<>();

        String query = "SELECT * FROM groups";

        try {
            ResultSet res = databaseManager.query(query);

            while (res.next()) {
                int id = res.getInt("id");
                String name = res.getString("name");

                Group group = new Group(id, name);
                groups.add(group);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return groups;
    }

    public List<Product> getProducts() {
        List<Product> products = new ArrayList<>();

        String query = "SELECT * FROM products";

        try {
            ResultSet res = databaseManager.query(query);

            while (res.next()) {
                int id = res.getInt("id");
                String name = res.getString("name");
                int quantity = res.getInt("quantity");
                int price = res.getInt("value");

                Product product = new Product(id, name, price, quantity);
                products.add(product);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return products;
    }

    public Group getGroup(int id) {
        String query = "SELECT * FROM groups WHERE id = ?";

        try (ResultSet res = databaseManager.query(query, id)) {
            if (res.next()) {
                String name = res.getString("name");
                return new Group(id, name);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<Group> getProductGroups(int productId) {
        List<Group> groups = new ArrayList<>();

        String query = "SELECT * FROM group_products WHERE product_id = ?";

        try (ResultSet res = databaseManager.query(query, productId)) {
            while (res.next()) {
                int groupId = res.getInt("group_id");
                Group group = getGroup(groupId);
                if (group != null) {
                    groups.add(group);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return groups;
    }

    public List<Product> getGroupProducts(int groupId) {
        List<Product> products = new ArrayList<>();

        String query = "SELECT * FROM group_products WHERE group_id = ?";

        try (ResultSet res = databaseManager.query(query, groupId)) {
            while (res.next()) {
                int productId = res.getInt("product_id");
                Product product = getProduct(productId);
                if (product != null) {
                    products.add(product);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return products;
    }

    public Product getProduct(int id) {
        String query = "SELECT * FROM products WHERE id = ?";

        try (ResultSet res = databaseManager.query(query, id)) {
            if (res.next()) {
                String name = res.getString("name");
                int quantity = res.getInt("quantity");
                int price = res.getInt("value");

                return new Product(id, name, price, quantity);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean isProductInGroup(int groupId, int productId) {
        String query = "SELECT 1 FROM group_products WHERE group_id = ? AND product_id = ?";

        try (ResultSet res = databaseManager.query(query, groupId, productId)) {
            return res.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean doGroupExist(int groupId) {
        String query = "SELECT COUNT(*) FROM groups WHERE id = ?";
        try (ResultSet res = databaseManager.query(query, groupId)) {
            if (res.next()) {
                return res.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean doProductExist(int productId) {
        String query = "SELECT COUNT(*) FROM products WHERE id = ?";
        try (ResultSet res = databaseManager.query(query, productId)) {
            if (res.next()) {
                return res.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

}
