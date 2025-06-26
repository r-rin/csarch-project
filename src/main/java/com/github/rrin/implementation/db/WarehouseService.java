package com.github.rrin.implementation.db;

import com.github.rrin.dto.Group;
import com.github.rrin.dto.Product;
import com.github.rrin.interfaces.IDatabaseManager;
import com.github.rrin.util.MySQLOptions;
import com.github.rrin.util.ProductSearchFilters;
import com.github.rrin.util.SearchResult;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WarehouseService implements Closeable {

    IDatabaseManager databaseManager;

    public WarehouseService() throws SQLException {
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

    public WarehouseService(MySQLOptions options) throws SQLException {
        try {
            databaseManager = new MySQLManager(options);
            init();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void init() throws SQLException {
        String createProductsTable = """
                CREATE TABLE IF NOT EXISTS products (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(255) UNIQUE NOT NULL,
                        manufacturer VARCHAR(255) NOT NULL,
                        description TEXT NOT NULL,
                        value DECIMAL(10,2) NOT NULL,
                        quantity INT NOT NULL
                    );
                """;

        String createGroupsTable = """
                    CREATE TABLE IF NOT EXISTS goods_groups (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        description TEXT NOT NULL,
                        name VARCHAR(255) UNIQUE NOT NULL
                    );
                """;

        String createGroupProductsTable = """
                    CREATE TABLE IF NOT EXISTS group_products (
                        group_id INT NOT NULL,
                        product_id INT NOT NULL,
                        PRIMARY KEY (group_id, product_id),
                        FOREIGN KEY (group_id) REFERENCES goods_groups(id) ON DELETE CASCADE,
                        FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
                    );
                """;

        databaseManager.update(createProductsTable);
        databaseManager.update(createGroupsTable);
        databaseManager.update(createGroupProductsTable);

        System.out.println("Database initialized.");
    }

    public void clearDB() {
        try {
            databaseManager.update("DELETE FROM group_products");
            databaseManager.update("DELETE FROM goods_groups");
            databaseManager.update("DELETE FROM products");

            databaseManager.update("ALTER TABLE products AUTO_INCREMENT = 1");
            databaseManager.update("ALTER TABLE goods_groups AUTO_INCREMENT = 1");

            System.out.println("Database cleared.");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to clear database", e);
        }
    }

    public SearchResult<Product> searchProducts(ProductSearchFilters filters) {
        List<Object> params = new ArrayList<>();
        StringBuilder queryBuilder = new StringBuilder();
        StringBuilder countQueryBuilder = new StringBuilder();

        if (filters.getGroupId() != null) {
            queryBuilder.append("SELECT DISTINCT p.* FROM products p ")
                    .append("INNER JOIN group_products gp ON p.id = gp.product_id ");
            countQueryBuilder.append("SELECT COUNT(DISTINCT p.id) FROM products p ")
                    .append("INNER JOIN group_products gp ON p.id = gp.product_id ");
        } else {
            queryBuilder.append("SELECT p.* FROM products p ");
            countQueryBuilder.append("SELECT COUNT(p.id) FROM products p ");
        }

        List<String> conditions = new ArrayList<>();

        if (filters.getName() != null && !filters.getName().trim().isEmpty()) {
            conditions.add("p.name LIKE ?");
            params.add(filters.getName());
        }

        if (filters.getGroupId() != null) {
            conditions.add("gp.group_id = ?");
            params.add(filters.getGroupId());
        }

        if (filters.getMinPrice() != null) {
            conditions.add("p.value >= ?");
            params.add(filters.getMinPrice());
        }

        if (filters.getMaxPrice() != null) {
            conditions.add("p.value <= ?");
            params.add(filters.getMaxPrice());
        }

        if (filters.getMinQuantity() != null) {
            conditions.add("p.quantity >= ?");
            params.add(filters.getMinQuantity());
        }

        if (filters.getMaxQuantity() != null) {
            conditions.add("p.quantity <= ?");
            params.add(filters.getMaxQuantity());
        }

        if (!conditions.isEmpty()) {
            String whereClause = " WHERE " + String.join(" AND ", conditions);
            queryBuilder.append(whereClause);
            countQueryBuilder.append(whereClause);
        }

        // Get total count first
        int totalCount = 0;
        try (ResultSet countResult = databaseManager.query(countQueryBuilder.toString(), params.toArray())) {
            if (countResult.next()) {
                totalCount = countResult.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new SearchResult<>(new ArrayList<>(), 0, filters.getPage(), filters.getPageSize());
        }

        // Add ordering and pagination to main query
        queryBuilder.append(" ORDER BY p.name ASC");
        queryBuilder.append(" LIMIT ? OFFSET ?");
        params.add(filters.getPageSize());
        params.add(filters.getPage() * filters.getPageSize());

        List<Product> products = new ArrayList<>();
        try (ResultSet rs = databaseManager.query(queryBuilder.toString(), params.toArray())) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                double value = rs.getDouble("value");
                int quantity = rs.getInt("quantity");
                String description = rs.getString("description");
                String manufacturer = rs.getString("manufacturer");

                Product product = new Product(id, name, manufacturer, description, value, quantity);
                products.add(product);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new SearchResult<>(products, totalCount, filters.getPage(), filters.getPageSize());
    }

    public int createProduct(String name, String manufacturer, String description, double price, int quantity) {
        String insertProductSQL = "INSERT INTO products (name, value, quantity, manufacturer, description) VALUES (?, ?, ?, ?, ?)";
        Connection connection = databaseManager.getConnection();

        boolean productExists = doProductNameExist(name);
        if (productExists) {return -1;}

        try (PreparedStatement ps = connection.prepareStatement(insertProductSQL, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setDouble(2, price);
            ps.setInt(3, quantity);
            ps.setString(4, manufacturer);
            ps.setString(5, description);

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

    public int createGroup(String name, String description) {
        String insertGroupsSQL = "INSERT INTO goods_groups (name, description) VALUES (?, ?)";
        Connection connection = databaseManager.getConnection();

        boolean groupExists = doGroupNameExist(name);
        if (groupExists) {return -1;}

        try (PreparedStatement ps = connection.prepareStatement(insertGroupsSQL, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, description);

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

        boolean productNameExist = doProductNameExist(name);
        if (productNameExist) {return false;}

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

    public boolean setProductManufacturer(int id, String manufacturer) {
        boolean productExist = doProductExist(id);
        if (!productExist) {return false;}

        String query = "UPDATE products SET manufacturer = ? WHERE id = ?";

        try {
            int res = databaseManager.update(query, manufacturer, id);
            if (res > 0) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean setProductDescription(int id, String description) {
        boolean productExist = doProductExist(id);
        if (!productExist) {return false;}

        String query = "UPDATE products SET description = ? WHERE id = ?";

        try {
            int res = databaseManager.update(query, description, id);
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

        boolean groupNameExist = doGroupNameExist(name);
        if (groupNameExist) {return false;}

        String query = "UPDATE goods_groups SET name = ? WHERE id = ?";

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

    public boolean setGroupDescription(int id, String description) {
        boolean groupExist = doGroupExist(id);
        if (!groupExist) {return false;}

        String query = "UPDATE goods_groups SET description = ? WHERE id = ?";

        try {
            int res = databaseManager.update(query, description, id);
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
        String query = "DELETE FROM goods_groups WHERE id = ?";

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

        String query = "SELECT * FROM goods_groups";

        try {
            ResultSet res = databaseManager.query(query);

            while (res.next()) {
                int id = res.getInt("id");
                String name = res.getString("name");
                String description = res.getString("description");

                Group group = new Group(id, name, description);
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
                String description = res.getString("description");
                String manufacturer = res.getString("manufacturer");

                Product product = new Product(id, name, manufacturer, description, price, quantity);
                products.add(product);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return products;
    }

    public Group getGroup(int id) {
        String query = "SELECT * FROM goods_groups WHERE id = ?";

        try (ResultSet res = databaseManager.query(query, id)) {
            if (res.next()) {
                String name = res.getString("name");
                String description = res.getString("description");
                return new Group(id, name, description);
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
                double price = res.getDouble("value");
                String description = res.getString("description");
                String manufacturer = res.getString("manufacturer");

                return new Product(id, name, manufacturer, description, price, quantity);
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
        String query = "SELECT COUNT(*) FROM goods_groups WHERE id = ?";
        try (ResultSet res = databaseManager.query(query, groupId)) {
            if (res.next()) {
                return res.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean doGroupNameExist(String name) {
        String query = "SELECT COUNT(*) FROM goods_groups WHERE name = ?";
        try (ResultSet res = databaseManager.query(query, name)) {
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
                boolean result = res.getInt(1) > 0;
                res.close();
                return result;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean doProductNameExist(String name) {
        String query = "SELECT COUNT(*) FROM products WHERE name = ?";
        try (ResultSet res = databaseManager.query(query, name)) {
            if (res.next()) {
                boolean result = res.getInt(1) > 0;
                res.close();
                return result;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void close() throws IOException {
        databaseManager.close();
    }
}
