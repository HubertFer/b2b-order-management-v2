package com.humberto.b2b.ordermanagement.order.application.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface DebitPartnerCreditPort {

    boolean debitIfAvailable(UUID partnerId, BigDecimal amount);
}
