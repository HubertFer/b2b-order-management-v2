package com.humberto.b2b.ordermanagement.partner.application;

import com.humberto.b2b.ordermanagement.partner.domain.Partner;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PartnerServicesTest {

    @Test
    void createsPartnerWithGeneratedIdAndNoUsedCredit() {
        List<Partner> saved = new ArrayList<>();
        CreatePartnerService service = new CreatePartnerService(partner -> {
            saved.add(partner);
            return partner;
        });

        Partner partner = service.create(new BigDecimal("500.00"));

        assertNotNull(partner.id());
        assertEquals(new BigDecimal("500.00"), partner.creditLimit());
        assertEquals(BigDecimal.ZERO, partner.usedCredit());
        assertEquals(List.of(partner), saved);
    }

    @Test
    void getsExistingPartnerOrFailsWhenMissing() {
        Partner partner = Partner.create(UUID.randomUUID(), BigDecimal.TEN);
        GetPartnerService found = new GetPartnerService(id -> Optional.of(partner));
        GetPartnerService missing = new GetPartnerService(id -> Optional.empty());

        assertSame(partner, found.getById(partner.id()));
        assertThrows(PartnerNotFoundException.class, () -> missing.getById(UUID.randomUUID()));
    }
}
