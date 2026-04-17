# DST Spring Services

Three Spring Boot microservices. The cart-service orchestrates checkouts by calling the other two.

| Service | Port |
|---|---|
| cart-service | 8080 |
| order-service | 8081 |
| payment-service | 8082 |

**Prerequisites:** Java 21, Docker

---

## Order Service

### Build

```bash
cd order-service
./gradlew build
```

### Run

```bash
docker-compose up --build
```

### Verify

```bash
# Create an order
curl -s -X POST http://localhost:8081/order \
  -H "Content-Type: text/plain" \
  -d "order-123"
# Expected: Order created

# Get order count (resets to 0 after each call)
curl -s http://localhost:8081/order-count
# Expected: 1
```

---

## Payment Service

### Build

```bash
cd payment-service
./gradlew build
```

### Run

`be.idempotent` is a required property. Set `BE_IDEMPOTENT=true` to enable duplicate-payment protection,
or `false` to simulate a buggy service that charges on every request.

```bash
BE_IDEMPOTENT=true docker-compose up --build
```

### Verify

```bash
# Initiate a payment
curl -s -X POST http://localhost:8082/payment \
  -H "Content-Type: text/plain" \
  -d "order-123"
# Expected: Payment initiated

# Verify idempotency — same order-id a second time should not increment the count
curl -s -X POST http://localhost:8082/payment \
  -H "Content-Type: text/plain" \
  -d "order-123"

# Get payment count (resets to 0 after each call)
curl -s http://localhost:8082/payment-count
# Expected: 1
```

---

## Cart Service

Cart service depends on order-service and payment-service. Start those first.

### Build

```bash
cd cart-service
./gradlew build
```

### Run

The combined `docker-compose.yml` in `cart-service/` starts all three services together.
Build each service's image from its own directory first so the compose file can use them:

```bash
# Build images (run from repo root)
(cd order-service   && ./gradlew build && docker build -t example/order-service:0.0.1 .)
(cd payment-service && ./gradlew build && docker build -t example/payment-service:0.0.1 .)
(cd cart-service    && ./gradlew build && docker build -t example/cart-service:0.0.1 .)
```

Then start everything:

```bash
cd cart-service
BE_IDEMPOTENT=true MAX_ATTEMPTS=3 docker-compose up
```

`MAX_ATTEMPTS` controls how many times the cart-service retries the payment service on failure.

### Verify

```bash
# Trigger a full checkout (creates order + initiates payment)
curl -s -X POST http://localhost:8080/checkout \
  -H "Content-Type: text/plain" \
  -d "order-42"
# Expected: Cart checked out in <N> ms

# Confirm both downstream services were called
curl -s http://localhost:8081/order-count    # Expected: 1
curl -s http://localhost:8082/payment-count  # Expected: 1
```
