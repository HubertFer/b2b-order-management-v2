package com.humberto.b2b.ordermanagement.order.application;

import java.math.BigDecimal;
import java.util.UUID;

public class InsufficientOrderCreditException extends RuntimeException {

    private final BigDecimal availableCredit;
    private final BigDecimal requestedAmount;

    public InsufficientOrderCreditException(UUID partnerId, BigDecimal availableCredit, BigDecimal requestedAmount) {
        super("Partner " + partnerId + " does not have enough available credit: available "
                + availableCredit + ", requested " + requestedAmount);
        this.availableCredit = availableCredit;
        this.requestedAmount = requestedAmount;
    }

    public BigDecimal availableCredit() {
        return availableCredit;
    }

    public BigDecimal requestedAmount() {
        return requestedAmount;
    }
}
