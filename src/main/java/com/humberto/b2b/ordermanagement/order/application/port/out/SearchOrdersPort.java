package com.humberto.b2b.ordermanagement.order.application.port.out;

import com.humberto.b2b.ordermanagement.order.application.OrderPage;
import com.humberto.b2b.ordermanagement.order.application.OrderSearchCriteria;

public interface SearchOrdersPort {

    OrderPage search(OrderSearchCriteria criteria, int page, int size);
}
