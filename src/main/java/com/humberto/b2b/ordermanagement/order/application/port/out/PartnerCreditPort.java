package com.humberto.b2b.ordermanagement.order.application.port.out;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface PartnerCreditPort {

    Optional<BigDecimal> findAvailableCredit(UUID partnerId);
}
