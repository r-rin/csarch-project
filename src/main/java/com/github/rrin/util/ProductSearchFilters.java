package com.github.rrin.util;

public class ProductSearchFilters {
    private String name;
    private Integer groupId;
    private Double minPrice;
    private Double maxPrice;
    private Integer minQuantity;
    private Integer maxQuantity;
    private int page = 0;
    private int pageSize = 5;

    public ProductSearchFilters() {}

    public ProductSearchFilters(String name, Integer groupId, Double minPrice, Double maxPrice, Integer minQuantity, Integer maxQuantity, int page, int pageSize) {
        this.name = name;
        this.groupId = groupId;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.minQuantity = minQuantity;
        this.maxQuantity = maxQuantity;
        this.page = page;
        this.pageSize = pageSize;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public Double getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(Double minPrice) {
        this.minPrice = minPrice;
    }

    public Double getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(Double maxPrice) {
        this.maxPrice = maxPrice;
    }

    public Integer getMinQuantity() {
        return minQuantity;
    }

    public void setMinQuantity(Integer minQuantity) {
        this.minQuantity = minQuantity;
    }

    public Integer getMaxQuantity() {
        return maxQuantity;
    }

    public void setMaxQuantity(Integer maxQuantity) {
        this.maxQuantity = maxQuantity;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
