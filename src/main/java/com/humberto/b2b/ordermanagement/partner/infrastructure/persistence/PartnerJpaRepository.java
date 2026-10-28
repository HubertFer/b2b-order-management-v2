package com.humberto.b2b.ordermanagement.partner.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

interface PartnerJpaRepository extends JpaRepository<PartnerEntity, UUID> {

    @Query("select p.creditLimit - p.usedCredit from PartnerEntity p where p.id = :id")
    Optional<BigDecimal> findAvailableCredit(@Param("id") UUID id);
}
