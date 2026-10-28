CREATE TABLE outbox_event (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id       UUID         NOT NULL UNIQUE,
    aggregate_type VARCHAR(50)  NOT NULL,
    aggregate_id   UUID         NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    payload        JSONB        NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL,
    published_at   TIMESTAMPTZ
);

CREATE INDEX ix_outbox_event_pending ON outbox_event (id) WHERE published_at IS NULL;
CREATE INDEX ix_outbox_event_aggregate ON outbox_event (aggregate_type, aggregate_id);
