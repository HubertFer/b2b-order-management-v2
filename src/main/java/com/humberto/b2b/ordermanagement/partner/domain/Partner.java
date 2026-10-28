package com.humberto.b2b.ordermanagement.partner.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record Partner(UUID id, BigDecimal creditLimit, BigDecimal usedCredit) {

    public Partner {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(creditLimit, "creditLimit must not be null");
        Objects.requireNonNull(usedCredit, "usedCredit must not be null");
        if (creditLimit.signum() < 0) {
            throw new IllegalArgumentException("creditLimit must not be negative");
        }
        if (usedCredit.signum() < 0) {
            throw new IllegalArgumentException("usedCredit must not be negative");
        }
        if (usedCredit.compareTo(creditLimit) > 0) {
            throw new IllegalArgumentException("usedCredit must not exceed creditLimit");
        }
    }

    public static Partner create(UUID id, BigDecimal creditLimit) {
        return new Partner(id, creditLimit, BigDecimal.ZERO);
    }

    public BigDecimal availableCredit() {
        return creditLimit.subtract(usedCredit);
    }

    public boolean hasAvailableCredit(BigDecimal amount) {
        Objects.requireNonNull(amount, "amount must not be null");
        return availableCredit().compareTo(amount) >= 0;
    }

    public Partner debit(BigDecimal amount) {
        Objects.requireNonNull(amount, "amount must not be null");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        if (!hasAvailableCredit(amount)) {
            throw new InsufficientCreditException(availableCredit(), amount);
        }
        return new Partner(id, creditLimit, usedCredit.add(amount));
    }
}