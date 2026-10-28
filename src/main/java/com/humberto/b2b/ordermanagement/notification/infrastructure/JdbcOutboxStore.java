package com.humberto.b2b.ordermanagement.notification.infrastructure;

import com.humberto.b2b.ordermanagement.notification.outbox.OutboxMessage;
import com.humberto.b2b.ordermanagement.notification.outbox.OutboxStore;
import com.humberto.b2b.ordermanagement.notification.outbox.PendingOutboxMessage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Component
class JdbcOutboxStore implements OutboxStore {

    private final JdbcTemplate jdbcTemplate;

    JdbcOutboxStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void append(OutboxMessage message) {
        jdbcTemplate.update("""
                        INSERT INTO outbox_event (event_id, aggregate_type, aggregate_id, event_type, payload, created_at)
                        VALUES (?, ?, ?, ?, ?::jsonb, ?)
                        """,
                message.eventId(), message.aggregateType(), message.aggregateId(), message.eventType(),
                message.payload(), toOffsetDateTime(message.occurredAt()));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public List<PendingOutboxMessage> lockPending(int limit) {
        return jdbcTemplate.query("""
                        SELECT id, event_id, aggregate_type, aggregate_id, event_type, payload::text AS payload, created_at
                          FROM outbox_event
                         WHERE published_at IS NULL
                         ORDER BY id
                         LIMIT ?
                           FOR UPDATE SKIP LOCKED
                        """,
                (rs, rowNum) -> new PendingOutboxMessage(rs.getLong("id"), new OutboxMessage(
                        rs.getObject("event_id", UUID.class),
                        rs.getString("aggregate_type"),
                        rs.getObject("aggregate_id", UUID.class),
                        rs.getString("event_type"),
                        rs.getString("payload"),
                        rs.getObject("created_at", OffsetDateTime.class).toInstant())),
                limit);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void markPublished(List<Long> ids, Instant publishedAt) {
        if (ids.isEmpty()) {
            return;
        }
        jdbcTemplate.update("UPDATE outbox_event SET published_at = ? WHERE id = ANY (?)", statement -> {
            statement.setObject(1, toOffsetDateTime(publishedAt));
            statement.setArray(2, statement.getConnection().createArrayOf("bigint", ids.toArray()));
        });
    }

    private static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
