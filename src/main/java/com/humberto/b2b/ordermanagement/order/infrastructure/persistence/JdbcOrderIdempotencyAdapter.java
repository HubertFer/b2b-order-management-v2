package com.humberto.b2b.ordermanagement.order.infrastructure.persistence;

import com.humberto.b2b.ordermanagement.order.application.IdempotencyRecord;
import com.humberto.b2b.ordermanagement.order.application.port.out.OrderIdempotencyPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
class JdbcOrderIdempotencyAdapter implements OrderIdempotencyPort {

    private final JdbcTemplate jdbcTemplate;

    JdbcOrderIdempotencyAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<IdempotencyRecord> find(UUID partnerId, String idempotencyKey) {
        return jdbcTemplate.query("""
                        SELECT partner_id, idempotency_key, request_hash, order_id
                          FROM order_idempotency_key
                         WHERE partner_id = ? AND idempotency_key = ?
                        """,
                (rs, rowNum) -> new IdempotencyRecord(
                        rs.getObject("partner_id", UUID.class),
                        rs.getString("idempotency_key"),
                        rs.getString("request_hash"),
                        rs.getObject("order_id", UUID.class)),
                partnerId, idempotencyKey).stream().findFirst();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean claim(IdempotencyRecord record) {
        return jdbcTemplate.update("""
                        INSERT INTO order_idempotency_key (partner_id, idempotency_key, request_hash, order_id)
                        VALUES (?, ?, ?, ?)
                        ON CONFLICT (partner_id, idempotency_key) DO NOTHING
                        """,
                record.partnerId(), record.idempotencyKey(), record.requestHash(), record.orderId()) == 1;
    }
}
