package com.humberto.b2b.ordermanagement;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DatabaseSchemaTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void rejectsPartnerWithUsedCreditAboveLimit() {
        assertThrows(DataIntegrityViolationException.class,
                () -> insertPartner(new BigDecimal("100.00"), new BigDecimal("100.01")));
    }

    @Test
    void rejectsPartnerWithNegativeUsedCredit() {
        assertThrows(DataIntegrityViolationException.class,
                () -> insertPartner(new BigDecimal("100.00"), new BigDecimal("-0.01")));
    }

    @Test
    void rejectsOrderWithUnknownStatus() {
        UUID partnerId = insertPartner(new BigDecimal("100.00"), BigDecimal.ZERO);

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "INSERT INTO orders (id, partner_id, total, status, created_at, updated_at) "
                        + "VALUES (?, ?, 10.00, 'DESCONHECIDO', now(), now())",
                UUID.randomUUID(), partnerId));
    }

    private UUID insertPartner(BigDecimal creditLimit, BigDecimal usedCredit) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO partner (id, credit_limit, used_credit) VALUES (?, ?, ?)",
                id, creditLimit, usedCredit);
        return id;
    }
}
