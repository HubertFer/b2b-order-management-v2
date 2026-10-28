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
        UUID partnerId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO partner (id, credit_limit, used_credit) VALUES (?, 100.00, 35.25)", partnerId);

        assertEquals(Optional.of(new BigDecimal("64.75")), adapter.findAvailableCredit(partnerId));
    }

    @Test
    void returnsEmptyForUnknownPartner() {
        assertEquals(Optional.empty(), adapter.findAvailableCredit(UUID.randomUUID()));
    }
}
