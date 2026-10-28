package com.humberto.b2b.ordermanagement.partner.infrastructure.persistence;

import com.humberto.b2b.ordermanagement.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class PartnerPersistenceAdapterTest {

    @Autowired
    private PartnerPersistenceAdapter adapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void findsAvailableCreditAsLimitMinusUsedCredit() {
        UUID partnerId = insertPartner("100.00", "35.25");

        assertEquals(Optional.of(new BigDecimal("64.75")), adapter.findAvailableCredit(partnerId));
    }

    @Test
    void returnsEmptyForUnknownPartner() {
        assertEquals(Optional.empty(), adapter.findAvailableCredit(UUID.randomUUID()));
    }

    @Test
    void debitsUpToExactlyTheAvailableCredit() {
        UUID partnerId = insertPartner("100.00", "70.00");

        assertTrue(adapter.debitIfAvailable(partnerId, new BigDecimal("30.00")));
        assertEquals(new BigDecimal("100.00"), usedCredit(partnerId));
    }

    @Test
    void doesNotDebitAboveAvailableCredit() {
        UUID partnerId = insertPartner("100.00", "70.00");

        assertFalse(adapter.debitIfAvailable(partnerId, new BigDecimal("30.01")));
        assertEquals(new BigDecimal("70.00"), usedCredit(partnerId));
    }

    @Test
    void doesNotDebitUnknownPartnerAndRejectsNonPositiveAmount() {
        UUID partnerId = insertPartner("100.00", "0");

        assertFalse(adapter.debitIfAvailable(UUID.randomUUID(), BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> adapter.debitIfAvailable(partnerId, BigDecimal.ZERO));
    }

    private UUID insertPartner(String creditLimit, String usedCredit) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO partner (id, credit_limit, used_credit) VALUES (?, ?, ?)",
                id, new BigDecimal(creditLimit), new BigDecimal(usedCredit));
        return id;
    }

    private BigDecimal usedCredit(UUID partnerId) {
        return jdbcTemplate.queryForObject("SELECT used_credit FROM partner WHERE id = ?", BigDecimal.class, partnerId);
    }
}
