package com.ecommerce.inventory.shared.responses;

import java.util.List;

public class PagedResponse<T> {

    private List<T> items;
    private int page;
    private int limit;
    private long total;
    private int totalPages;

    public static <T> PagedResponse<T> of(List<T> items, int page, int limit, long total) {
        PagedResponse<T> response = new PagedResponse<>();
        response.items = items;
        response.page = page;
        response.limit = limit;
        response.total = total;
        response.totalPages = limit == 0 ? 0 : (int) Math.ceil((double) total / limit);
        return response;
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}
