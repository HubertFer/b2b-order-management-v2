package com.humberto.b2b.ordermanagement.order.application;

import com.humberto.b2b.ordermanagement.order.application.port.out.FindOrderPort;
import com.humberto.b2b.ordermanagement.order.domain.Order;

import java.util.UUID;

public class GetOrderService {

    private final FindOrderPort findOrderPort;

    public GetOrderService(FindOrderPort findOrderPort) {
        this.findOrderPort = findOrderPort;
    }

    public Order getById(UUID orderId) {
        return findOrderPort.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}
