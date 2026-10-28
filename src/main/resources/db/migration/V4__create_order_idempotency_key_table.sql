CREATE TABLE order_idempotency_key (
    partner_id      UUID         NOT NULL REFERENCES partner (id),
    idempotency_key VARCHAR(100) NOT NULL,
    request_hash    CHAR(64)     NOT NULL,
    order_id        UUID         NOT NULL REFERENCES orders (id) DEFERRABLE INITIALLY DEFERRED,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (partner_id, idempotency_key)
);
