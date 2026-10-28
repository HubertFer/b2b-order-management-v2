# B2B Order Management

Order management service for B2B partners with credit limits. Orders are created, approved against the partner's
credit, moved through their lifecycle and cancelled with credit refund. Every status change is published to Kafka
through a transactional outbox.

## Tech stack

- Java 21, Spring Boot 4.1 (Web MVC, Data JPA, Validation, Actuator, Spring Kafka)
- PostgreSQL 17 with Flyway migrations
- Apache Kafka 4.1 (KRaft)
- JUnit 5, Testcontainers (PostgreSQL and Kafka), ArchUnit, k6

## Running

```bash
docker compose up --build
```

This starts PostgreSQL, Kafka and the application on `http://localhost:8081`, with two demo partners:

| Partner id | Credit limit |
|---|---|
| `11111111-1111-1111-1111-111111111111` | 10000.00 |
| `22222222-2222-2222-2222-222222222222` | 500.00 |

Health probes: `/actuator/health/liveness` and `/actuator/health/readiness`.

### Tests

```bash
./mvnw verify
```

Docker must be running: integration tests use real PostgreSQL and Kafka containers.

### Load test

```bash
docker compose --profile load-test run --rm k6
```

## API

| Method | Path | Success | Errors |
|---|---|---|---|
| `POST` | `/partners` | 201 | 400 |
| `GET` | `/partners/{id}` | 200 with `availableCredit` | 404 |
| `POST` | `/orders` | 201, or 200 when replayed by `Idempotency-Key` | 400, 422 |
| `GET` | `/orders?status=&createdFrom=&createdTo=&page=&size=` | 200 | 400 |
| `GET` | `/orders/{id}` | 200 | 404 |
| `POST` | `/orders/{id}/approve` | 200 | 404, 409, 422 |
| `POST` | `/orders/{id}/cancel` | 200 | 404, 409 |
| `PATCH` | `/orders/{id}/status` | 200 | 400, 404, 409, 422 |

Errors use RFC 9457 `ProblemDetail`. Validation errors list each invalid field in `errors`. Insufficient credit
returns the partner's current available credit:

```json
{
  "status": 422,
  "detail": "Partner ... does not have enough available credit: available 300.00, requested 400.00",
  "availableCredit": 300.00,
  "requestedAmount": 400.00
}
```

`availableCredit` is a snapshot taken when the request was rejected; it may change right after.

### Example

```bash
curl -X POST localhost:8081/orders -H 'Content-Type: application/json' -H 'Idempotency-Key: order-001' \
  -d '{"partnerId":"22222222-2222-2222-2222-222222222222","items":[{"productId":"33333333-3333-3333-3333-333333333333","quantity":2,"unitPrice":100.00}]}'

curl -X POST localhost:8081/orders/{id}/approve
curl localhost:8081/partners/22222222-2222-2222-2222-222222222222
```

## Business rules

### Lifecycle

```
PENDENTE -> APROVADO -> EM_PROCESSAMENTO -> ENVIADO -> ENTREGUE
   |           |               |
CANCELADO  CANCELADO       CANCELADO
```

`ENTREGUE` and `CANCELADO` are final. Any other transition returns 409.

### Credit

- Creating an order checks that the partner has enough available credit, without debiting it.
- Approving debits the order total. Cancelling an approved order refunds it.
- `availableCredit = creditLimit - usedCredit` is always computed, never stored.

### Concurrency

Three layers protect the credit limit:

1. `SELECT ... FOR UPDATE` on the order serializes every status change of the same order, so an order is
   approved, cancelled or refunded only once.
2. The debit is a conditional update that checks and debits in one statement:
   `UPDATE partner SET used_credit = used_credit + :amount WHERE id = :id AND credit_limit - used_credit >= :amount`.
   Concurrent approvals for the same partner wait on the row and re-evaluate the condition.
3. `CHECK (used_credit <= credit_limit)` rejects anything that gets past the first two.

All approvals lock the order before the partner, so lock order is consistent and cannot deadlock.

### Idempotent order creation

`Idempotency-Key` (optional, 1 to 100 printable ASCII characters) is unique per partner. The service stores the key
with a SHA-256 fingerprint of the request (partner, and items sorted so their order does not matter):

- same key and same request: returns the original order with 200;
- same key and different request: 422;
- concurrent requests with the same key: the unique key serializes them, one creates the order and the others
  replay it.

### Notifications

Each status change appends an event to `outbox_event` in the same transaction as the change, so an event exists
if and only if the change was committed. A relay publishes pending events to the `order-status-changed` topic:

- `FOR UPDATE SKIP LOCKED` lets several instances relay in parallel without publishing the same event twice at the
  same time;
- the relay stops at the first failure, keeping per-order event order, and retries later;
- the message key is the order id, so events of one order stay in one partition and in order;
- the producer uses `acks=all` and idempotence.

Delivery is at least once. The consumer stores each `eventId` in `processed_event` in the same transaction as the
notification and ignores duplicates. Messages that keep failing go to `order-status-changed-dlt`.

Without Kafka (`notification.publisher=log`, the default outside Docker Compose) events are written to the log.

## Architecture

Modules `order`, `partner` and `notification`, each split into layers:

- `domain`: business rules in plain Java;
- `application`: use cases and the ports they need;
- `infrastructure`: JPA, JDBC and Kafka adapters, and the Spring configuration that wires use cases;
- `api`: controllers, DTOs and exception handlers.

The `order` module does not depend on `partner`: it asks for credit through its own `PartnerCreditPort`, implemented
by the partner persistence adapter. `ArchitectureTest` enforces layer direction, a framework-free domain and this
module boundary.

## Load test results

Local run with Docker Compose, 1 minute: 100 order lifecycles per second (create, idempotent retry, approve, fetch),
25 searches per second and 50 users competing for one partner's credit.

- about 430 requests per second, 0 failures, p95 under 10 ms;
- the contested partner ended with exactly its credit limit used and never above it;
- every outbox event was published to Kafka and consumed.

## Before production

- Cleanup jobs for old rows in `order_idempotency_key`, `processed_event` and published `outbox_event`.
- Authentication, and checking that a caller can only act on its own partner.
- Event schema registry (Avro or Protobuf) to version the event contract.
- Kafka and PostgreSQL with replication, and connection pool sizing based on measured load.
- Metrics and alerts for outbox backlog, consumer lag and the dead letter topic.
