package com.humberto.b2b.ordermanagement.partner.infrastructure.persistence;

import com.humberto.b2b.ordermanagement.order.application.port.out.PartnerCreditPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Component
class PartnerPersistenceAdapter implements PartnerCreditPort {

    private final PartnerJpaRepository repository;

    PartnerPersistenceAdapter(PartnerJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BigDecimal> findAvailableCredit(UUID partnerId) {
        return repository.findAvailableCredit(partnerId);
    }
}
