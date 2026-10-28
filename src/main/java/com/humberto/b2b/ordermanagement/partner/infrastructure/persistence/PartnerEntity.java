package com.humberto.b2b.ordermanagement.partner.infrastructure.persistence;

import com.humberto.b2b.ordermanagement.partner.domain.Partner;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "partner")
public class PartnerEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "credit_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "used_credit", nullable = false, precision = 19, scale = 2)
    private BigDecimal usedCredit;

    @Transient
    private boolean isNew;

    protected PartnerEntity() {
    }

    static PartnerEntity fromDomain(Partner partner) {
        PartnerEntity entity = new PartnerEntity();
        entity.id = partner.id();
        entity.creditLimit = partner.creditLimit();
        entity.usedCredit = partner.usedCredit();
        entity.isNew = true;
        return entity;
    }

    Partner toDomain() {
        return new Partner(id, creditLimit, usedCredit);
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }
}
