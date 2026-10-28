package com.humberto.b2b.ordermanagement.order.application;

import com.humberto.b2b.ordermanagement.order.application.port.out.FindOrderPort;
import com.humberto.b2b.ordermanagement.order.application.port.out.OrderIdempotencyPort;
import com.humberto.b2b.ordermanagement.order.application.port.out.PartnerCreditPort;
import com.humberto.b2b.ordermanagement.order.application.port.out.SaveOrderPort;
import com.humberto.b2b.ordermanagement.order.domain.Order;
import com.humberto.b2b.ordermanagement.order.domain.OrderItem;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CreateOrderService {

    private final PartnerCreditPort partnerCreditPort;
    private final SaveOrderPort saveOrderPort;
    private final FindOrderPort findOrderPort;
    private final OrderIdempotencyPort orderIdempotencyPort;
    private final Clock clock;

    public CreateOrderService(PartnerCreditPort partnerCreditPort,
                              SaveOrderPort saveOrderPort,
                              FindOrderPort findOrderPort,
                              OrderIdempotencyPort orderIdempotencyPort,
                              Clock clock) {
        this.partnerCreditPort = partnerCreditPort;
        this.saveOrderPort = saveOrderPort;
        this.findOrderPort = findOrderPort;
        this.orderIdempotencyPort = orderIdempotencyPort;
        this.clock = clock;
    }

    @Transactional
    public Order create(UUID partnerId, List<OrderItem> items) {
        return saveOrderPort.save(newOrder(partnerId, items));
    }

    @Transactional
    public OrderCreation create(UUID partnerId, List<OrderItem> items, String idempotencyKey) {
        if (idempotencyKey == null) {
            return OrderCreation.created(create(partnerId, items));
        }
        String requestHash = OrderRequestFingerprint.of(partnerId, items);
        Optional<OrderCreation> replay = findReplay(partnerId, idempotencyKey, requestHash);
        if (replay.isPresent()) {
            return replay.get();
        }
        Order order = newOrder(partnerId, items);
        if (!orderIdempotencyPort.claim(new IdempotencyRecord(partnerId, idempotencyKey, requestHash, order.id()))) {
            return findReplay(partnerId, idempotencyKey, requestHash)
                    .orElseThrow(() -> new IllegalStateException("Idempotency key " + idempotencyKey + " was not stored"));
        }
        return OrderCreation.created(saveOrderPort.save(order));
    }

    private Optional<OrderCreation> findReplay(UUID partnerId, String idempotencyKey, String requestHash) {
        return orderIdempotencyPort.find(partnerId, idempotencyKey).map(record -> {
            if (!record.requestHash().equals(requestHash)) {
                throw new IdempotencyKeyReusedException(idempotencyKey);
            }
            Order existing = findOrderPort.findById(record.orderId())
                    .orElseThrow(() -> new IllegalStateException("Order " + record.orderId() + " was not found"));
            return OrderCreation.replayed(existing);
        });
    }

    private Order newOrder(UUID partnerId, List<OrderItem> items) {
        BigDecimal availableCredit = partnerCreditPort.findAvailableCredit(partnerId)
                .orElseThrow(() -> new PartnerNotFoundException(partnerId));
        Order order = Order.create(partnerId, items, clock.instant());
        if (availableCredit.compareTo(order.total()) < 0) {
            throw new InsufficientOrderCreditException(partnerId, availableCredit, order.total());
        }
        return order;
    }
}
