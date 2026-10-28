package com.humberto.b2b.ordermanagement.partner.infrastructure.persistence;

import com.humberto.b2b.ordermanagement.order.application.port.out.DebitPartnerCreditPort;
import com.humberto.b2b.ordermanagement.order.application.port.out.PartnerCreditPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
class PartnerPersistenceAdapter implements PartnerCreditPort, DebitPartnerCreditPort {

    private final PartnerJpaRepository repository;

    PartnerPersistenceAdapter(PartnerJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BigDecimal> findAvailableCredit(UUID partnerId) {
        return repository.findAvailableCredit(partnerId);
    }

    @Override
    @Transactional
    public boolean debitIfAvailable(UUID partnerId, BigDecimal amount) {
        Objects.requireNonNull(partnerId, "partnerId must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        return repository.debitIfAvailable(partnerId, amount) == 1;
    }
}
