package com.humberto.b2b.ordermanagement.order.application.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface RefundPartnerCreditPort {

    void refund(UUID partnerId, BigDecimal amount);
}
