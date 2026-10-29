# B2B Order Management

[![CI](https://github.com/HubertFer/b2b-order-management-v2/actions/workflows/ci.yml/badge.svg)](https://github.com/HubertFer/b2b-order-management-v2/actions/workflows/ci.yml)

Service for managing B2B orders: creation, lookup, status lifecycle, partner credit control and status change
notifications published to Kafka.

## Tech stack

- Java 21, Spring Boot 4.1, Maven
- PostgreSQL 17, Spring Data JPA, Flyway
- Apache Kafka 4.1 (KRaft), Spring Kafka
- springdoc-openapi (Swagger UI)
- JUnit 5, MockMvc, Testcontainers (PostgreSQL and Kafka), ArchUnit
- Docker Compose, k6, GitHub Actions

## Running

Requirement: Docker.

```bash
docker compose up --build
```

Compose starts PostgreSQL, Kafka and the application at `http://localhost:8081`. The application only starts once
PostgreSQL and Kafka report healthy.

If ports `8081` or `5432` are already in use on your machine, choose other host ports:

```bash
APP_PORT=9090 POSTGRES_PORT=15432 docker compose up --build
```

The Compose environment seeds two demo partners (`db/demo/R__demo_partners.sql`):

| Partner | Credit limit |
|---|---|
| `11111111-1111-1111-1111-111111111111` | 10,000.00 |
| `22222222-2222-2222-2222-222222222222` | 500.00 |

This data is only loaded when `SPRING_FLYWAY_LOCATIONS` includes `classpath:db/demo`, as in `compose.yaml`. The
versioned migrations in `db/migration` contain no data. New partners can be registered with `POST /partners`.

To stop and remove all data: `docker compose down -v`.

## Swagger UI

**http://localhost:8081/swagger-ui/index.html**

The whole API can be explored and called from the browser:

- Every endpoint has a summary, a description of its rules and every response it can return (`200`, `201`, `400`,
  `404`, `409`, `422`), with the `ProblemDetail` error format.
- Request examples work as-is against the Compose environment. "Try it out" on `POST /orders` creates an order for
  the demo partner `22222222-2222-2222-2222-222222222222`, and `GET /partners/{id}` shows its credit right after.
- The optional `Idempotency-Key` header is documented on `POST /orders`: send the same request twice with the same
  key to see the second call return `200` with the original order.
- The OpenAPI document is at http://localhost:8081/v3/api-docs, for importing into Postman, Insomnia or a client
  generator.

`OpenApiDocumentationTest` fails the build if an endpoint disappears from the document or the Swagger UI stops
loading.

Other endpoints:

- Health: http://localhost:8081/actuator/health, with `/liveness` and `/readiness` probes. The `app` service
  healthcheck uses the readiness probe, so `docker compose up --wait` returns only when the API can serve requests.

### Running the application outside a container

Requirements: Java 21 and Docker. Useful for development and debugging.

**With temporary containers (Testcontainers):**

```bash
./mvnw spring-boot:test-run
```

A throwaway PostgreSQL container is started and removed automatically. It starts empty, without demo partners, and
notifications go to the log instead of Kafka (`notification.publisher=log`).

**With the Compose database and Kafka:**

```bash
docker compose up -d --wait postgres kafka

SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/orders \
SPRING_DATASOURCE_USERNAME=orders \
SPRING_DATASOURCE_PASSWORD=orders \
SPRING_FLYWAY_LOCATIONS=classpath:db/migration,classpath:db/demo \
./mvnw spring-boot:run
```

Kafka is only reachable from inside the Compose network, so this mode keeps the default log publisher. Keep
`SPRING_FLYWAY_LOCATIONS` the same as in `compose.yaml`: once the demo partners are loaded, Flyway refuses to start
without the `db/demo` location.

### Tests

```bash
./mvnw verify
```

Integration tests start PostgreSQL and Kafka with Testcontainers, so Docker must be running. The same command runs on
every push and pull request through GitHub Actions (`.github/workflows/ci.yml`).

## API

| Method | Path | Description |
|---|---|---|
| `POST` | `/partners` | Registers a partner with a credit limit |
| `GET` | `/partners/{id}` | Gets a partner with its credit limit, used credit and available credit |
| `POST` | `/orders` | Creates an order (status `PENDENTE`); accepts an optional `Idempotency-Key` header |
| `GET` | `/orders/{id}` | Gets an order |
| `GET` | `/orders?status=&createdFrom=&createdTo=&page=&size=` | Paginated search by status and creation period |
| `POST` | `/orders/{id}/approve` | Approves the order and debits the partner's credit |
| `PATCH` | `/orders/{id}/status` | Moves the order to any status allowed by the lifecycle; `APROVADO` and `CANCELADO` apply the same credit rules as `/approve` and `/cancel` |
| `POST` | `/orders/{id}/cancel` | Cancels the order and refunds the credit if it was debited |

Errors use the `ProblemDetail` format (RFC 9457) with content type `application/problem+json`.

| Status | When |
|---|---|
| 400 | Invalid body, header or parameters, or a field that is not part of the request |
| 404 | Order or partner not found |
| 409 | Status transition not allowed |
| 422 | Partner not found, not enough available credit, or `Idempotency-Key` reused with a different request |

When the credit is not enough, the response says how much was available and how much was requested, so the client
can split the order or ask for a higher limit:

```json
{
  "title": "Unprocessable Content",
  "status": 422,
  "detail": "Partner 22222222-2222-2222-2222-222222222222 does not have enough available credit: available 300.00, requested 400.00",
  "instance": "/orders",
  "availableCredit": 300.00,
  "requestedAmount": 400.00
}
```

`availableCredit` is the balance when the request was rejected; concurrent approvals may change it right after.

Request bodies are strict: a field that is not part of the request, such as `total` sent to `POST /orders`, is
rejected with `400` instead of being silently ignored. Validation errors list the fields that failed:

```json
{
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failure",
  "instance": "/orders",
  "errors": [
    {"field": "items[0].quantity", "message": "must be greater than 0"},
    {"field": "items[0].unitPrice", "message": "numeric value out of bounds (<17 digits>.<2 digits> expected)"}
  ]
}
```

### Walkthrough

Every step can also be run from the [Swagger UI](http://localhost:8081/swagger-ui/index.html). The walkthrough uses
the demo partner seeded by Compose; to use your own partner, register it in step 1 and use the returned `id` as
`partnerId`. Replace `{orderId}` with the `id` returned in step 2.

**1. Register a partner** (or use the demo partner `22222222-2222-2222-2222-222222222222`)

```bash
curl -X POST http://localhost:8081/partners \
  -H 'Content-Type: application/json' \
  -d '{"creditLimit": 1000.00}'
```

`201 Created`

```json
{"id": "8f0b95e7-8c92-43fb-adb0-0a5f13023db8", "creditLimit": 1000.00, "usedCredit": 0.00, "availableCredit": 1000.00}
```

**2. Create an order** (`Idempotency-Key` is optional)

```bash
curl -X POST http://localhost:8081/orders \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: 7f3c2a90-order-001' \
  -d '{
        "partnerId": "22222222-2222-2222-2222-222222222222",
        "items": [
          {"productId": "33333333-3333-3333-3333-333333333333", "quantity": 2, "unitPrice": 100.00}
        ]
      }'
```

`201 Created` (repeating the same request with the same key returns `200 OK` with the same order)

```json
{
  "id": "0a3dff20-b5b1-458b-be17-16bdf4436a95",
  "partnerId": "22222222-2222-2222-2222-222222222222",
  "items": [{"productId": "33333333-3333-3333-3333-333333333333", "quantity": 2, "unitPrice": 100.00}],
  "total": 200.00,
  "status": "PENDENTE",
  "createdAt": "2026-10-28T12:00:00Z",
  "updatedAt": "2026-10-28T12:00:00Z"
}
```

**3. Approve it** (debits the partner's credit)

```bash
curl -X POST http://localhost:8081/orders/{orderId}/approve
```

`200 OK` with the order in status `APROVADO`. `PATCH /orders/{orderId}/status` with `{"status": "APROVADO"}` does the
same. About a second later the event has gone through the outbox and Kafka, and the consumer logs it
(`docker compose logs app`):

```
Partner notified: orderId=0a3dff20-... payload={"eventId": "...", "orderId": "...", "previousStatus": "PENDENTE", "newStatus": "APROVADO", ...}
```

**4. Check the credit**

```bash
curl http://localhost:8081/partners/22222222-2222-2222-2222-222222222222
```

```json
{"id": "22222222-2222-2222-2222-222222222222", "creditLimit": 500.00, "usedCredit": 200.00, "availableCredit": 300.00}
```

**5. Try to exceed the credit**

```bash
curl -X POST http://localhost:8081/orders \
  -H 'Content-Type: application/json' \
  -d '{"partnerId": "22222222-2222-2222-2222-222222222222", "items": [{"productId": "33333333-3333-3333-3333-333333333333", "quantity": 4, "unitPrice": 100.00}]}'
```

`422` with `"availableCredit": 300.00` and `"requestedAmount": 400.00`.

**6. Move it forward or cancel it**

```bash
curl -X PATCH http://localhost:8081/orders/{orderId}/status \
  -H 'Content-Type: application/json' -d '{"status": "EM_PROCESSAMENTO"}'

curl -X POST http://localhost:8081/orders/{orderId}/cancel
```

Cancelling an approved or processing order returns the `200.00` to the partner's available credit.

**7. Search**

```bash
curl 'http://localhost:8081/orders?status=CANCELADO&createdFrom=2026-10-01T00:00:00Z&createdTo=2026-11-01T00:00:00Z&page=0&size=20'
```

```json
{"content": [{"id": "0a3dff20-b5b1-458b-be17-16bdf4436a95", "status": "CANCELADO", "...": "..."}], "page": 0, "size": 20, "totalElements": 1, "totalPages": 1}
```

## Business rules

### Statuses

Status values are kept in Portuguese, as defined in the challenge: `PENDENTE` (pending), `APROVADO` (approved),
`EM_PROCESSAMENTO` (processing), `ENVIADO` (shipped), `ENTREGUE` (delivered) and `CANCELADO` (cancelled).

```
PENDENTE ──► APROVADO ──► EM_PROCESSAMENTO ──► ENVIADO ──► ENTREGUE
   │            │                │
   └────────────┴────────────────┴──► CANCELADO
```

`ENTREGUE` and `CANCELADO` are final. Any other transition, including moving to the current status, returns 409.

### Credit

- **Amounts:** prices and credit limits have at most two decimal places; requests with more are rejected with `400`
  instead of being rounded. Responses always show amounts with two decimal places.
- **Limits:** an order has at most 100 items, each with a quantity of at most 1,000,000, and its total must fit the
  `NUMERIC(19,2)` columns (17 integer digits). Larger orders are rejected with `400`.
- **Creation:** checks that the available credit (`limit - used`) covers the order total. Nothing is debited.
- **Approval:** debits the order total from the partner's credit.
- **Cancellation:** if the order had already been approved (`APROVADO` or `EM_PROCESSAMENTO`), the total is returned
  to the partner's available credit. Cancelling a `PENDENTE` order does not change the credit.
- **Lookup:** `GET /partners/{id}` returns `availableCredit`, always computed as `creditLimit - usedCredit`.

`PATCH /status` accepts every status. `APROVADO` and `CANCELADO` are delegated to the same use cases as
`POST /approve` and `POST /cancel`, so the credit is debited or refunded the same way whichever endpoint is used.

### Concurrency

Approval must prevent negative balances and double approvals, even under concurrent requests:

1. The order is read with `SELECT ... FOR UPDATE`. Approvals and cancellations of the same order are serialized, so a
   second request sees the new status and is rejected with 409.
2. The debit is a single conditional `UPDATE`:
   ```sql
   update partner set used_credit = used_credit + :amount
    where id = :id and credit_limit - used_credit >= :amount
   ```
   If no row is updated, there is not enough credit and the whole transaction is rolled back. Because the check and
   the debit happen in the same statement, concurrent approvals for the same partner cannot exceed the limit:
   PostgreSQL makes the second update wait for the first and re-evaluates the condition on the new balance.
3. A `CHECK` constraint (`used_credit >= 0 AND used_credit <= credit_limit`) is the final guarantee in the database.

Refunds follow the same pattern, with `used_credit >= :amount` as the condition. Every status change locks the order
before the partner, so locks are always taken in the same order and cannot deadlock.

`ApproveOrderConcurrencyTest` and `CancelOrderConcurrencyTest` run parallel approvals and cancellations against a real
PostgreSQL instance: ten approvals of 30.00 for a partner with 100.00 approve exactly three, five approvals of the same
order debit once, and an approval racing a cancellation always ends `CANCELADO` with no credit used.

### Idempotent order creation

Clients retrying a `POST /orders` after a timeout must not create duplicate orders. Sending an `Idempotency-Key` header
(1 to 100 printable ASCII characters, unique per partner) makes the request safe to retry:

- **First request:** the order is created and the response is `201`.
- **Same key and same request:** the order created by the first request is returned with `200`, even if the partner
  no longer has available credit.
- **Same key with a different request:** `422`.

Requests are compared by a SHA-256 fingerprint of the partner and items, stored with the key. Items are sorted before
hashing, so the same items in another order are the same request, and prices are normalized, so `10` and `10.00` match.

The key is reserved with `INSERT ... ON CONFLICT DO NOTHING` before the order is saved, in the same transaction. When
two requests with the same key arrive at the same time, the second one waits for the first to commit and then returns
the same order; the foreign key to `orders` is `DEFERRABLE INITIALLY DEFERRED` so the key can be reserved first.
`IdempotentCreationConcurrencyTest` sends ten concurrent requests with the same key and checks that one order is
created.

### Notifications (outbox and Kafka)

Every status change writes an `ORDER_STATUS_CHANGED` event to the `outbox_event` table **in the same transaction** that
changes the order. If the operation fails, no event is written; once it commits, the event cannot be lost.

A scheduled relay reads pending events in order with `FOR UPDATE SKIP LOCKED`, so several instances can run at the
same time without publishing the same event twice, and publishes each batch to the `order-status-changed` topic:

- **Ordering:** the message key is the order id, so all events of an order go to the same partition and are consumed
  in order.
- **Durability:** the producer uses `acks=all` and idempotence; a resend after a timeout is discarded by the broker.
- **Throughput:** a batch is sent at once and its acknowledgements are awaited in order. Events are marked published
  only up to the first failure; the rest stay pending and are retried on the next run, so nothing is lost and the
  order is kept.

Delivery is *at-least-once*. The consumer (`OrderStatusChangedListener`) stores each `eventId` in `processed_event`
with `INSERT ... ON CONFLICT DO NOTHING`, in the same transaction as the partner notification, and skips events it
has already processed. A message that fails three times goes to `order-status-changed-dlt`, so a bad message does not
block its partition. `KafkaNotificationTest` follows an approval from the outbox through a real Kafka broker to the
consumer, and checks that a duplicated message notifies the partner once.

| Property | Default | Description |
|---|---|---|
| `notification.publisher` | `log` | `kafka` in Compose; `log` writes events to the log, for tests and local runs |
| `notification.kafka.topic` | `order-status-changed` | Topic for status events; failed messages go to `<topic>-dlt` |
| `notification.kafka.partitions` | `3` | Partitions created for the topic |
| `notification.kafka.send-timeout` | `PT5S` | Maximum wait for the broker to acknowledge a batch |
| `notification.outbox.relay-interval` | `PT1S` | Pause between relay runs once the backlog is empty |
| `notification.outbox.batch-size` | `50` | Events locked, published and marked per transaction |
| `notification.outbox.drain-timeout` | `PT10S` | Maximum time a single run keeps draining full batches |

## Runtime configuration

| Property | Value | Why |
|---|---|---|
| `spring.threads.virtual.enabled` | `true` | Requests and background tasks run on Java 21 virtual threads, so threads blocked on database I/O do not limit how many concurrent requests the service accepts |
| `spring.datasource.hikari.maximum-pool-size` | `20` | Upper bound on database connections per instance. With virtual threads this pool becomes the real concurrency limit for database work |
| `spring.datasource.hikari.minimum-idle` | `5` | Keeps connections ready for traffic spikes |
| `spring.datasource.hikari.connection-timeout` | `3000` (ms) | Fails fast under overload instead of piling up waiting requests |
| `spring.jpa.open-in-view` | `false` | Connections are released when the transaction ends, not when the response is written |
| `spring.jackson.deserialization.fail-on-unknown-properties` | `true` | Unknown request fields are rejected instead of ignored |
| `spring.kafka.producer.acks` | `all` | The broker acknowledges only after the in-sync replicas have the message |
| `management.endpoints.web.exposure.include` | `health` | Only the health endpoint is exposed over HTTP |

Every property can be overridden with an environment variable, for example `SPRING_DATASOURCE_HIKARI_MAXIMUMPOOLSIZE=40`.

## Scalability

The service is designed to run as several identical instances behind a load balancer, with PostgreSQL as the single
source of truth.

- **Stateless instances.** No session or in-memory state is shared between requests. Any instance can serve any
  request, and the readiness probe tells the load balancer when an instance can receive traffic.
- **Fine-grained locking.** There is no global lock. Approvals and cancellations lock only the order being changed,
  and the partner credit is updated by a single conditional `UPDATE`. Orders from different partners never wait for
  each other.
- **Safe retries.** `Idempotency-Key` lets clients and gateways retry `POST /orders` after timeouts without creating
  duplicates, even when the retries land on different instances.
- **Outbox across instances.** Every instance runs the relay. `FOR UPDATE SKIP LOCKED` lets them split pending events
  instead of publishing the same event twice or waiting for each other.
- **Parallel consumers.** The topic has three partitions keyed by order id, so up to three consumer instances share
  the load while each order's events stay in order.
- **Efficient reads.** Searches are paginated (maximum page size of 100) and backed by a composite index on
  `(status, created_at)`, plus indexes on `created_at` and `partner_id`. Order items of a page are loaded in batches
  instead of one query per order, and read endpoints use read-only transactions.
- **Bounded resources.** Virtual threads accept many concurrent requests cheaply, while the connection pool caps the
  load each instance puts on the database.

### Load test

A [k6](https://k6.io) script in `load-test/orders.js` runs as a Compose service, so it needs nothing besides Docker:

```bash
docker compose --profile load-test run --rm k6
docker compose --profile load-test run --rm -e RATE=500 -e DURATION=2m k6
```

It runs three scenarios at the same time:

| Scenario | What it does |
|---|---|
| `order_lifecycle` | `RATE` iterations per second (default 100), each creating an order with an `Idempotency-Key`, retrying the same request, approving the order and reading it back, spread across 50 partners |
| `search` | `RATE / 4` paginated searches per second by status |
| `hot_partner` | 50 concurrent users sending 500 create and approve attempts for a single partner whose credit only covers 100 of them |

The run fails if more than 1% of requests return an unexpected status, if the 95th percentile latency exceeds 500 ms,
if the hot partner ends with its used credit above (or below) its limit, or if a credit rejection does not report
`availableCredit` and `requestedAmount`.

Results on a 24-core development machine, with the application, PostgreSQL, Kafka and k6 all running in Docker on the
same host:

| Load | Requests | Throughput | p50 / p95 `order_lifecycle` | p50 / p95 `search` | Failed requests | Hot partner | Events not yet published at the end |
|---|---|---|---|---|---|---|---|
| `RATE=100`, 1 min | 26,193 | ~430 req/s | 4.0 / 8.6 ms | 3.9 / 8.6 ms | 0% | 100 approved, 400 rejected, used credit 1000 of 1000 | 0 of 6,101 |
| `RATE=500`, 2 min | 255,459 | ~2,120 req/s | 4.8 / 7.4 ms | 4.6 / 9.6 ms | 0% | 100 approved, 400 rejected, used credit 1000 of 1000 | 0 of 60,046 |

These numbers come from a single instance with default settings, sharing the machine with the database, the broker
and k6, and are meant to show behavior under concurrency, not a production benchmark. The relay keeps up with the
order traffic: the outbox is empty when the test ends. At `RATE=500` the consumer, which processes about 500 events
per second with one transaction per message, ended the test with a backlog in Kafka and caught up about a minute later.

### Limits and next steps

- **Database connections.** The number of instances times `maximum-pool-size` must stay below PostgreSQL's
  `max_connections` (100 by default). Beyond that, a connection pooler such as PgBouncer is the next step.
- **Hot partners.** Approvals for the same partner serialize on that partner's row. This is the price of never
  exceeding the credit limit; each approval holds the row only for a few milliseconds.
- **Consumer throughput.** The consumer handles one message per transaction. Batch listening, or more partitions and
  consumer instances, would raise its throughput.
- **Read scaling.** Searches could be moved to a read replica if read traffic outgrows the primary database.

## Architecture

Modular monolith organized by feature, with layers inside each module:

```
order/
  api/
    controller/    REST controller
    dto/           request and response records
    handler/       exception to ProblemDetail mapping
  application/     use cases and outbound ports (port/out)
  domain/          Order, OrderItem, OrderStatus, transitions and events
  infrastructure/  JPA persistence, idempotency keys, outbox adapter and use case wiring
partner/
  api/             controller, dto and handler, as in order
  application/     use cases and outbound ports (port/out)
  domain/          Partner
  infrastructure/  persistence, atomic credit operations and use case wiring
notification/
  outbox/          relay and storage/publishing contracts
  inbox/           deduplicated handling of received events
  infrastructure/  JDBC storage, scheduling, Kafka producer and consumer
```

- The domain is immutable and does not depend on Spring. Transitions return a new `Order` instance.
- Use cases depend only on ports. Infrastructure adapters implement those ports, and the services are registered in
  `OrderUseCaseConfiguration` and `PartnerUseCaseConfiguration`, with no Spring stereotype annotations in the
  application layer.
- The `order` module does not know the `partner` module. It asks for available credit through its own
  `PartnerCreditPort`, which returns an amount, and the partner persistence adapter implements it. Splitting the
  modules into services would replace that adapter with a remote call.
- `ArchitectureTest` (ArchUnit) enforces these rules, so a dependency in the wrong direction fails the build: layer
  direction, a domain that depends only on Java, use cases without Spring stereotypes or persistence, web and Kafka
  dependencies, and no dependency from `order` on `partner`.

## Decisions and limitations

- **Partner management** is limited to registration and balance lookup. The credit limit cannot be changed after
  registration.
- **Order creation** does not trigger a notification, since it is not a status change.
- **Retention:** idempotency keys, published outbox events and processed event ids are kept indefinitely. A
  production setup would delete them after a retention period, such as 24 hours for keys and 7 days for events.
- **Authentication** is not implemented. A production setup would also check that a caller only acts on its own
  partner's orders.
- **Event contract:** events are JSON without a schema registry. A shared contract would use Avro or Protobuf with a
  registry to version it.
- **Kafka in Compose** runs as a single broker with replication factor 1; production would use at least three brokers.
- **Swagger UI** is enabled by default. In production it can be disabled with `springdoc.api-docs.enabled=false` and
  `springdoc.swagger-ui.enabled=false`.
