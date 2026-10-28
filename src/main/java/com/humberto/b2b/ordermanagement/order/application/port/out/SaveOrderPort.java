package com.humberto.b2b.ordermanagement.order.application.port.out;

import com.humberto.b2b.ordermanagement.order.domain.Order;

public interface SaveOrderPort {

    Order save(Order order);
}
