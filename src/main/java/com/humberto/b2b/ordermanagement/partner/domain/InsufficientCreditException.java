package com.humberto.b2b.ordermanagement.partner.domain;

import java.math.BigDecimal;

public class InsufficientCreditException extends RuntimeException {

    private final BigDecimal availableCredit;
    private final BigDecimal requestedAmount;

    public InsufficientCreditException(BigDecimal availableCredit, BigDecimal requestedAmount) {
        super("Insufficient available credit: available " + availableCredit + ", requested " + requestedAmount);
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
