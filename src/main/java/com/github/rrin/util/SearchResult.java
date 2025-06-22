package com.github.rrin.util;

import java.util.List;

public class SearchResult<T> {

    private final List<T> items;
    private final int totalCount;
    private final int currentPage;
    private final int pageSize;
    private final int totalPages;

    public SearchResult(List<T> items, int totalCount, int currentPage, int pageSize) {
        this.items = items;
        this.totalCount = totalCount;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalPages = (int) Math.ceil((double) totalCount / pageSize);
    }

    public List<T> getItems() {
        return items;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getTotalPages() {
        return totalPages;
    }
}
