package com.humberto.b2b.ordermanagement.order.application.port.out;

import com.humberto.b2b.ordermanagement.order.domain.OrderStatusChangedEvent;

public interface RecordOrderStatusChangePort {

    void record(OrderStatusChangedEvent event);
}
