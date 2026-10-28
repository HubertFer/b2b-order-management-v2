package com.humberto.b2b.ordermanagement.order.application;

import com.humberto.b2b.ordermanagement.order.application.port.out.SearchOrdersPort;

import java.util.Objects;

public class SearchOrdersService {

    public static final int MAX_PAGE_SIZE = 100;

    private final SearchOrdersPort searchOrdersPort;

    public SearchOrdersService(SearchOrdersPort searchOrdersPort) {
        this.searchOrdersPort = searchOrdersPort;
    }

    public OrderPage search(OrderSearchCriteria criteria, int page, int size) {
        Objects.requireNonNull(criteria, "criteria must not be null");
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        return searchOrdersPort.search(criteria, page, size);
    }
}
