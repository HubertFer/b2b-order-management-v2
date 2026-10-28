package com.humberto.b2b.ordermanagement.order.infrastructure.notification;

import com.humberto.b2b.ordermanagement.notification.outbox.OutboxMessage;
import com.humberto.b2b.ordermanagement.notification.outbox.OutboxStore;
import com.humberto.b2b.ordermanagement.order.application.port.out.RecordOrderStatusChangePort;
import com.humberto.b2b.ordermanagement.order.domain.OrderStatusChangedEvent;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
class OrderStatusChangeOutboxAdapter implements RecordOrderStatusChangePort {

    static final String AGGREGATE_TYPE = "ORDER";
    static final String EVENT_TYPE = "ORDER_STATUS_CHANGED";

    private final OutboxStore outboxStore;
    private final JsonMapper jsonMapper;

    OrderStatusChangeOutboxAdapter(OutboxStore outboxStore, JsonMapper jsonMapper) {
        this.outboxStore = outboxStore;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void record(OrderStatusChangedEvent event) {
        outboxStore.append(new OutboxMessage(
                event.eventId(),
                AGGREGATE_TYPE,
                event.orderId(),
                EVENT_TYPE,
                jsonMapper.writeValueAsString(event),
                event.occurredAt()
        ));
    }
}
