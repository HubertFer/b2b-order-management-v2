package com.humberto.b2b.ordermanagement.notification.inbox;

import java.util.UUID;

public interface PartnerNotifier {

    void notifyOrderStatusChanged(UUID orderId, String payload);
}
