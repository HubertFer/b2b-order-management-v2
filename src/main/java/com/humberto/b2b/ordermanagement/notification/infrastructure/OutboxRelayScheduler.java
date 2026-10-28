package com.humberto.b2b.ordermanagement.notification.infrastructure;

import com.humberto.b2b.ordermanagement.notification.outbox.OutboxRelay;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
class OutboxRelayScheduler {

    private final OutboxRelay outboxRelay;
    private final Duration drainTimeout;

    OutboxRelayScheduler(OutboxRelay outboxRelay,
                         @Value("${notification.outbox.drain-timeout:PT10S}") Duration drainTimeout) {
        this.outboxRelay = outboxRelay;
        this.drainTimeout = drainTimeout;
    }

    @Scheduled(fixedDelayString = "${notification.outbox.relay-interval:PT1S}")
    void relayPending() {
        outboxRelay.drain(drainTimeout);
    }
}
