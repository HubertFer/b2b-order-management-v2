package com.humberto.b2b.ordermanagement.notification.infrastructure;

import com.humberto.b2b.ordermanagement.notification.inbox.PartnerNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
class LoggingPartnerNotifier implements PartnerNotifier {

    private static final Logger log = LoggerFactory.getLogger(LoggingPartnerNotifier.class);

    @Override
    public void notifyOrderStatusChanged(UUID orderId, String payload) {
        log.info("Partner notified: orderId={} payload={}", orderId, payload);
    }
}
