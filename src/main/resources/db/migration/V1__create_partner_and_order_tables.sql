CREATE TABLE partner (
                         id           UUID PRIMARY KEY,
                         credit_limit NUMERIC(19, 2) NOT NULL,
                         used_credit  NUMERIC(19, 2) NOT NULL DEFAULT 0,
                         CONSTRAINT ck_partner_credit_limit CHECK (credit_limit >= 0),
                         CONSTRAINT ck_partner_used_credit CHECK (used_credit >= 0 AND used_credit <= credit_limit)
);

CREATE TABLE orders (
                        id         UUID PRIMARY KEY,
                        partner_id UUID           NOT NULL REFERENCES partner (id),
                        total      NUMERIC(19, 2) NOT NULL,
                        status     VARCHAR(30)    NOT NULL,
                        created_at TIMESTAMPTZ    NOT NULL,
                        updated_at TIMESTAMPTZ    NOT NULL,
                        CONSTRAINT ck_orders_total CHECK (total >= 0),
                        CONSTRAINT ck_orders_status CHECK (status IN
                                                           ('PENDENTE', 'APROVADO', 'EM_PROCESSAMENTO', 'ENVIADO', 'ENTREGUE', 'CANCELADO'))
);

CREATE INDEX ix_orders_partner_id ON orders (partner_id);
CREATE INDEX ix_orders_status_created_at ON orders (status, created_at);
CREATE INDEX ix_orders_created_at ON orders (created_at);

CREATE TABLE order_item (
                            order_id   UUID           NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
                            product_id UUID           NOT NULL,
                            quantity   INTEGER        NOT NULL,
                            unit_price NUMERIC(19, 2) NOT NULL,
                            CONSTRAINT ck_order_item_quantity CHECK (quantity > 0),
                            CONSTRAINT ck_order_item_unit_price CHECK (unit_price >= 0)
);

CREATE INDEX ix_order_item_order_id ON order_item (order_id);