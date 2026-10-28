package com.humberto.b2b.ordermanagement.notification.infrastructure;

import com.humberto.b2b.ordermanagement.notification.inbox.ProcessedEventStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

@Component
class JdbcProcessedEventStore implements ProcessedEventStore {

    private final JdbcTemplate jdbcTemplate;

    JdbcProcessedEventStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean markProcessed(UUID eventId, Instant processedAt) {
        return jdbcTemplate.update("""
                        INSERT INTO processed_event (event_id, processed_at)
                        VALUES (?, ?)
                        ON CONFLICT (event_id) DO NOTHING
                        """,
                eventId, processedAt.atOffset(ZoneOffset.UTC)) == 1;
    }
}
