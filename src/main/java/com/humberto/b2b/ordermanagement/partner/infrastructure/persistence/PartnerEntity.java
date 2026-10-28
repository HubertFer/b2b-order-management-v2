package com.humberto.b2b.ordermanagement.partner.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "partner")
public class PartnerEntity {

    @Id
    private UUID id;

    @Column(name = "credit_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "used_credit", nullable = false, precision = 19, scale = 2)
    private BigDecimal usedCredit;

    protected PartnerEntity() {
    }
}
