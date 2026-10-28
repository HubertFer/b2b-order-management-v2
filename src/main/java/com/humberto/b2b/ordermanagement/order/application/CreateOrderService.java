package com.humberto.b2b.ordermanagement.order.application;

import com.humberto.b2b.ordermanagement.order.application.port.out.PartnerCreditPort;
import com.humberto.b2b.ordermanagement.order.application.port.out.SaveOrderPort;
import com.humberto.b2b.ordermanagement.order.domain.Order;
import com.humberto.b2b.ordermanagement.order.domain.OrderItem;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

public class CreateOrderService {

    private final PartnerCreditPort partnerCreditPort;
    private final SaveOrderPort saveOrderPort;
    private final Clock clock;

    public CreateOrderService(PartnerCreditPort partnerCreditPort, SaveOrderPort saveOrderPort, Clock clock) {
        this.partnerCreditPort = partnerCreditPort;
        this.saveOrderPort = saveOrderPort;
        this.clock = clock;
    }

    public Order create(UUID partnerId, List<OrderItem> items) {
        BigDecimal availableCredit = partnerCreditPort.findAvailableCredit(partnerId)
                .orElseThrow(() -> new PartnerNotFoundException(partnerId));
        Order order = Order.create(partnerId, items, clock.instant());
        if (availableCredit.compareTo(order.total()) < 0) {
            throw new InsufficientOrderCreditException(partnerId, availableCredit, order.total());
        }
        return saveOrderPort.save(order);
    }
}
