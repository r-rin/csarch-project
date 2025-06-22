package com.github.rrin;

import com.github.rrin.dto.Group;
import com.github.rrin.dto.Product;
import com.github.rrin.implementation.db.WarehouseService;
import com.github.rrin.util.MySQLOptions;
import com.github.rrin.util.ProductSearchFilters;
import com.github.rrin.util.SearchResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class WarehouseServiceTest {

    private static final MySQLContainer<?> MY_SQL_CONTAINER = new MySQLContainer<>();

    private static WarehouseService warehouseService;

    @BeforeAll
    public static void startMySQLContainer() throws SQLException {
        MY_SQL_CONTAINER.start();
        MySQLOptions options = new MySQLOptions(
                MY_SQL_CONTAINER.getJdbcUrl(),
                MY_SQL_CONTAINER.getUsername(),
                MY_SQL_CONTAINER.getPassword()
        );
        warehouseService = new WarehouseService(options);
    }

    @AfterAll
    public static void stopMySQLContainer() {
        MY_SQL_CONTAINER.stop();
    }

    @BeforeEach
    public void setUp() {
        warehouseService.clearDB();
    }

    @Test
    public void testCreateProduct() {
        int productId = warehouseService.createProduct("Test Product", 99.99, 10);
        assertTrue(productId > 0, "Product ID should be positive");

        Product product = warehouseService.getProduct(productId);
        assertNotNull(product);
        assertEquals("Test Product", product.name());
        assertEquals(99.99, product.price(), 0.01);
        assertEquals(10, product.quantity());
    }

    @Test
    public void testGetProduct() {
        int productId = warehouseService.createProduct("Test Product", 50.0, 5);

        Product product = warehouseService.getProduct(productId);
        assertNotNull(product);
        assertEquals(productId, product.id());
        assertEquals("Test Product", product.name());
        assertEquals(50.0, product.price(), 0.01);
        assertEquals(5, product.quantity());
    }

    @Test
    public void testGetNonExistentProduct() {
        Product product = warehouseService.getProduct(999);
        assertNull(product);
    }

    @Test
    public void testGetAllProducts() {
        warehouseService.createProduct("Product 1", 10.0, 1);
        warehouseService.createProduct("Product 2", 20.0, 2);
        warehouseService.createProduct("Product 3", 30.0, 3);

        List<Product> products = warehouseService.getProducts();
        assertEquals(3, products.size());
    }

    @Test
    public void testUpdateProductName() {
        int productId = warehouseService.createProduct("Original Name", 10.0, 5);

        boolean updated = warehouseService.setProductName(productId, "Updated Name");
        assertTrue(updated);

        Product product = warehouseService.getProduct(productId);
        assertEquals("Updated Name", product.name());
    }

    @Test
    public void testUpdateProductPrice() {
        int productId = warehouseService.createProduct("Test Product", 10.0, 5);

        boolean updated = warehouseService.setProductPrice(productId, 25.50);
        assertTrue(updated);

        Product product = warehouseService.getProduct(productId);
        assertEquals(25.50, product.price(), 0.01);
    }

    @Test
    public void testUpdateProductQuantity() {
        int productId = warehouseService.createProduct("Test Product", 10.0, 5);

        boolean updated = warehouseService.setProductQuantities(productId, 15);
        assertTrue(updated);

        Product product = warehouseService.getProduct(productId);
        assertEquals(15, product.quantity());
    }

    @Test
    public void testUpdateNonExistentProduct() {
        boolean updated = warehouseService.setProductName(999, "New Name");
        assertFalse(updated);

        updated = warehouseService.setProductPrice(999, 10.0);
        assertFalse(updated);

        updated = warehouseService.setProductQuantities(999, 10);
        assertFalse(updated);
    }

    @Test
    public void testRemoveProduct() {
        int productId = warehouseService.createProduct("Test Product", 10.0, 5);

        Product removedProduct = warehouseService.removeProduct(productId);
        assertNotNull(removedProduct);
        assertEquals("Test Product", removedProduct.name());

        Product product = warehouseService.getProduct(productId);
        assertNull(product);
    }

    @Test
    public void testRemoveNonExistentProduct() {
        Product removedProduct = warehouseService.removeProduct(999);
        assertNull(removedProduct);
    }

    @Test
    public void testDoProductExist() {
        int productId = warehouseService.createProduct("Test Product", 10.0, 5);

        assertTrue(warehouseService.doProductExist(productId));
        assertFalse(warehouseService.doProductExist(999));
    }

    // Group CRUD Tests
    @Test
    public void testCreateGroup() {
        int groupId = warehouseService.createGroup("Test Group");
        assertTrue(groupId > 0, "Group ID should be positive");

        Group group = warehouseService.getGroup(groupId);
        assertNotNull(group);
        assertEquals("Test Group", group.name());
    }

    @Test
    public void testGetGroup() {
        int groupId = warehouseService.createGroup("Test Group");

        Group group = warehouseService.getGroup(groupId);
        assertNotNull(group);
        assertEquals(groupId, group.id());
        assertEquals("Test Group", group.name());
    }

    @Test
    public void testGetNonExistentGroup() {
        Group group = warehouseService.getGroup(999);
        assertNull(group);
    }

    @Test
    public void testGetAllGroups() {
        warehouseService.createGroup("Group 1");
        warehouseService.createGroup("Group 2");
        warehouseService.createGroup("Group 3");

        List<Group> groups = warehouseService.getGroups();
        assertEquals(3, groups.size());
    }

    @Test
    public void testUpdateGroupName() {
        int groupId = warehouseService.createGroup("Original Group");

        boolean updated = warehouseService.setGroupName(groupId, "Updated Group");
        assertTrue(updated);

        Group group = warehouseService.getGroup(groupId);
        assertEquals("Updated Group", group.name());
    }

    @Test
    public void testUpdateNonExistentGroup() {
        boolean updated = warehouseService.setGroupName(999, "New Name");
        assertFalse(updated);
    }

    @Test
    public void testRemoveGroup() {
        int groupId = warehouseService.createGroup("Test Group");

        Group removedGroup = warehouseService.removeGroup(groupId);
        assertNotNull(removedGroup);
        assertEquals("Test Group", removedGroup.name());

        Group group = warehouseService.getGroup(groupId);
        assertNull(group);
    }

    @Test
    public void testRemoveNonExistentGroup() {
        Group removedGroup = warehouseService.removeGroup(999);
        assertNull(removedGroup);
    }

    @Test
    public void testDoGroupExist() {
        int groupId = warehouseService.createGroup("Test Group");

        assertTrue(warehouseService.doGroupExist(groupId));
        assertFalse(warehouseService.doGroupExist(999));
    }

    // Group-Product Relationship Tests
    @Test
    public void testAddProductToGroup() {
        int productId = warehouseService.createProduct("Test Product", 10.0, 5);
        int groupId = warehouseService.createGroup("Test Group");

        boolean added = warehouseService.addProductToGroup(groupId, productId);
        assertTrue(added);

        assertTrue(warehouseService.isProductInGroup(groupId, productId));
    }

    @Test
    public void testAddProductToGroupTwice() {
        int productId = warehouseService.createProduct("Test Product", 10.0, 5);
        int groupId = warehouseService.createGroup("Test Group");

        warehouseService.addProductToGroup(groupId, productId);
        boolean addedSecondTime = warehouseService.addProductToGroup(groupId, productId);
        assertFalse(addedSecondTime, "Should not add same product to group twice");
    }

    @Test
    public void testAddNonExistentProductToGroup() {
        int groupId = warehouseService.createGroup("Test Group");

        boolean added = warehouseService.addProductToGroup(groupId, 999);
        assertFalse(added);
    }

    @Test
    public void testAddProductToNonExistentGroup() {
        int productId = warehouseService.createProduct("Test Product", 10.0, 5);

        boolean added = warehouseService.addProductToGroup(999, productId);
        assertFalse(added);
    }

    @Test
    public void testRemoveProductFromGroup() {
        int productId = warehouseService.createProduct("Test Product", 10.0, 5);
        int groupId = warehouseService.createGroup("Test Group");

        warehouseService.addProductToGroup(groupId, productId);
        boolean removed = warehouseService.removeProductFromGroup(groupId, productId);
        assertTrue(removed);

        assertFalse(warehouseService.isProductInGroup(groupId, productId));
    }

    @Test
    public void testRemoveProductNotInGroup() {
        int productId = warehouseService.createProduct("Test Product", 10.0, 5);
        int groupId = warehouseService.createGroup("Test Group");

        boolean removed = warehouseService.removeProductFromGroup(groupId, productId);
        assertTrue(removed, "Should return true even if product wasn't in group");
    }

    @Test
    public void testGetProductGroups() {
        int productId = warehouseService.createProduct("Test Product", 10.0, 5);
        int groupId1 = warehouseService.createGroup("Group 1");
        int groupId2 = warehouseService.createGroup("Group 2");

        warehouseService.addProductToGroup(groupId1, productId);
        warehouseService.addProductToGroup(groupId2, productId);

        List<Group> groups = warehouseService.getProductGroups(productId);
        assertEquals(2, groups.size());
    }

    @Test
    public void testGetGroupProducts() {
        int groupId = warehouseService.createGroup("Test Group");
        int productId1 = warehouseService.createProduct("Product 1", 10.0, 5);
        int productId2 = warehouseService.createProduct("Product 2", 20.0, 10);

        warehouseService.addProductToGroup(groupId, productId1);
        warehouseService.addProductToGroup(groupId, productId2);

        List<Product> products = warehouseService.getGroupProducts(groupId);
        assertEquals(2, products.size());
    }

    @Test
    public void testIsProductInGroup() {
        int productId = warehouseService.createProduct("Test Product", 10.0, 5);
        int groupId = warehouseService.createGroup("Test Group");

        assertFalse(warehouseService.isProductInGroup(groupId, productId));

        warehouseService.addProductToGroup(groupId, productId);
        assertTrue(warehouseService.isProductInGroup(groupId, productId));
    }

    // Search Tests
    @Test
    public void testSearchProductsNoFilters() {
        warehouseService.createProduct("Apple", 1.0, 10);
        warehouseService.createProduct("Banana", 2.0, 20);
        warehouseService.createProduct("Cherry", 3.0, 30);

        ProductSearchFilters filters = new ProductSearchFilters();
        filters.setPage(0);
        filters.setPageSize(10);

        SearchResult<Product> result = warehouseService.searchProducts(filters);
        assertEquals(3, result.getTotalCount());
        assertEquals(3, result.getItems().size());
    }

    @Test
    public void testSearchProductsByName() {
        warehouseService.createProduct("Apple Juice", 1.0, 10);
        warehouseService.createProduct("Apple Pie", 2.0, 20);
        warehouseService.createProduct("Banana", 3.0, 30);

        ProductSearchFilters filters = new ProductSearchFilters();
        filters.setName("Apple%");
        filters.setPage(0);
        filters.setPageSize(10);

        SearchResult<Product> result = warehouseService.searchProducts(filters);
        assertEquals(2, result.getTotalCount());
        assertEquals(2, result.getItems().size());
    }

    @Test
    public void testSearchProductsByPriceRange() {
        warehouseService.createProduct("Cheap", 5.0, 10);
        warehouseService.createProduct("Medium", 15.0, 20);
        warehouseService.createProduct("Expensive", 25.0, 30);

        ProductSearchFilters filters = new ProductSearchFilters();
        filters.setMinPrice(10.0);
        filters.setMaxPrice(20.0);
        filters.setPage(0);
        filters.setPageSize(10);

        SearchResult<Product> result = warehouseService.searchProducts(filters);
        assertEquals(1, result.getTotalCount());
        assertEquals("Medium", result.getItems().get(0).name());
    }

    @Test
    public void testSearchProductsByQuantityRange() {
        warehouseService.createProduct("Low Stock", 10.0, 5);
        warehouseService.createProduct("Medium Stock", 10.0, 15);
        warehouseService.createProduct("High Stock", 10.0, 25);

        ProductSearchFilters filters = new ProductSearchFilters();
        filters.setMinQuantity(10);
        filters.setMaxQuantity(20);
        filters.setPage(0);
        filters.setPageSize(10);

        SearchResult<Product> result = warehouseService.searchProducts(filters);
        assertEquals(1, result.getTotalCount());
        assertEquals("Medium Stock", result.getItems().get(0).name());
    }

    @Test
    public void testSearchProductsByGroup() {
        int groupId = warehouseService.createGroup("Electronics");
        int productId1 = warehouseService.createProduct("Phone", 500.0, 10);
        int productId2 = warehouseService.createProduct("Laptop", 1000.0, 5);
        int productId3 = warehouseService.createProduct("Book", 20.0, 50);

        warehouseService.addProductToGroup(groupId, productId1);
        warehouseService.addProductToGroup(groupId, productId2);

        ProductSearchFilters filters = new ProductSearchFilters();
        filters.setGroupId(groupId);
        filters.setPage(0);
        filters.setPageSize(10);

        SearchResult<Product> result = warehouseService.searchProducts(filters);
        assertEquals(2, result.getTotalCount());
        assertEquals(2, result.getItems().size());
    }

    @Test
    public void testSearchProductsPagination() {
        for (int i = 1; i <= 25; i++) {
            warehouseService.createProduct("Product " + i, i * 10.0, i);
        }

        ProductSearchFilters filters = new ProductSearchFilters();
        filters.setPage(1); // Second page
        filters.setPageSize(10);

        SearchResult<Product> result = warehouseService.searchProducts(filters);
        assertEquals(25, result.getTotalCount());
        assertEquals(10, result.getItems().size());
        assertEquals(1, result.getCurrentPage());
        assertEquals(10, result.getPageSize());
    }

    @Test
    public void testClearDB() {
        warehouseService.createProduct("Test Product", 10.0, 5);
        warehouseService.createGroup("Test Group");

        assertFalse(warehouseService.getProducts().isEmpty());
        assertFalse(warehouseService.getGroups().isEmpty());

        warehouseService.clearDB();

        assertTrue(warehouseService.getProducts().isEmpty());
        assertTrue(warehouseService.getGroups().isEmpty());
    }
}
