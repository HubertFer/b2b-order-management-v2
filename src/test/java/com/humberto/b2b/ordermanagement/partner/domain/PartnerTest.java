package com.humberto.b2b.ordermanagement.partner.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartnerTest {

    @Test
    void createsPartnerWithNoUsedCredit() {
        Partner partner = Partner.create(UUID.randomUUID(), new BigDecimal("100.00"));

        assertEquals(BigDecimal.ZERO, partner.usedCredit());
    }

    @Test
    void calculatesAvailableCredit() {
        Partner partner = new Partner(UUID.randomUUID(), new BigDecimal("100.00"), new BigDecimal("35.25"));

        assertEquals(new BigDecimal("64.75"), partner.availableCredit());
    }

    @Test
    void checksAvailableCreditWithoutDependingOnScale() {
        Partner partner = Partner.create(UUID.randomUUID(), new BigDecimal("10.00"));

        assertTrue(partner.hasAvailableCredit(new BigDecimal("10.000")));
        assertFalse(partner.hasAvailableCredit(new BigDecimal("10.01")));
    }

    @Test
    void debitsCreditByReturningANewPartner() {
        Partner partner = new Partner(UUID.randomUUID(), new BigDecimal("100.00"), new BigDecimal("20.00"));

        Partner debited = partner.debit(new BigDecimal("30.00"));

        assertNotSame(partner, debited);
        assertEquals(new BigDecimal("20.00"), partner.usedCredit());
        assertEquals(new BigDecimal("50.00"), debited.usedCredit());
        assertEquals(new BigDecimal("50.00"), debited.availableCredit());
    }

    @Test
    void rejectsDebitAboveAvailableCreditWithDetails() {
        Partner partner = new Partner(UUID.randomUUID(), new BigDecimal("100.00"), new BigDecimal("90.00"));

        InsufficientCreditException exception = assertThrows(InsufficientCreditException.class,
                () -> partner.debit(new BigDecimal("10.01")));

        assertEquals(new BigDecimal("10.00"), exception.availableCredit());
        assertEquals(new BigDecimal("10.01"), exception.requestedAmount());
    }

    @Test
    void rejectsNonPositiveDebitAndInvalidCreditValues() {
        Partner partner = Partner.create(UUID.randomUUID(), new BigDecimal("10.00"));
        UUID id = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () -> partner.debit(BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> partner.debit(new BigDecimal("-1")));
        assertThrows(IllegalArgumentException.class, () -> new Partner(id, new BigDecimal("-1"), BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new Partner(id, BigDecimal.TEN, new BigDecimal("-1")));
        assertThrows(IllegalArgumentException.class, () -> new Partner(id, BigDecimal.TEN, new BigDecimal("11")));
    }
}
