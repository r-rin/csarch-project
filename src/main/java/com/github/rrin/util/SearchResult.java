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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Search result:\n");
        sb.append(" - Total Count: ").append(totalCount).append("\n");
        sb.append(" - Current Page: ").append(currentPage).append(" of ").append(totalPages).append("\n");
        sb.append(" - Page Size: ").append(pageSize).append("\n");
        sb.append(" - Items:\n");

        if (items.isEmpty()) {
            sb.append("   (No items found)\n");
        } else {
            for (int i = 0; i < items.size(); i++) {
                sb.append("   ").append(i + 1).append(". ").append(items.get(i).toString()).append("\n");
            }
        }

        return sb.toString().trim();
    }
}
