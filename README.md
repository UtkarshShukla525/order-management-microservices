# Order Management System (Spring Boot Microservices)

A small but realistic microservices backend: customers browse products and place orders,
and the order service coordinates stock with the product service over REST.

## Architecture

```
                         +--------------------+
   Client / Postman ---> |    api-gateway     |  :8080   single entry point
                         +---------+----------+
                                   |  routes by path, resolves names via Eureka
                 +-----------------+------------------+
                 |                                    |
        /api/products/**                       /api/orders/**
                 |                                    |
        +--------v---------+     REST (reserve /  +----v-------------+
        |  product-service |<----- release) -----|   order-service  |
        |  :8081  (H2/MySQL)|                     |  :8082 (H2/MySQL)|
        +--------+---------+                     +----+-------------+
                 \                                    /
                  +--------->  discovery-server  <---+     :8761 (Eureka)
                               (service registry)
```

| Service            | Port | Responsibility                                              |
|--------------------|------|-------------------------------------------------------------|
| `discovery-server` | 8761 | Eureka registry; services find each other by name           |
| `api-gateway`      | 8080 | Spring Cloud Gateway; routes `/api/products/**` and `/api/orders/**` |
| `product-service`  | 8081 | Product CRUD and stock reservation/release                  |
| `order-service`    | 8082 | Place / view / cancel orders; calls product-service for stock |

Each business service has its **own database** (database-per-service), so they cannot join
tables or share a transaction. That is why order placement uses a reserve-then-save flow with a
compensating "release stock" step.

## Tech stack

Java 17, Spring Boot 3.3, Spring Web (REST), Spring Data JPA / Hibernate, Bean Validation,
Spring Cloud (Eureka, Gateway, LoadBalancer), `RestClient`, H2 (default) / MySQL, JUnit 5, Mockito, Maven.

## Prerequisites

- JDK 17 or newer
- Maven 3.9+
- (Optional) MySQL 8 if you want to run with the `mysql` profile

## Build and test

```bash
mvn clean verify
```

This compiles all four modules and runs the unit tests (service layer with Mockito, controller
layer with `@WebMvcTest`).

## Run

Start the services in this order, each in its own terminal, from the project root:

```bash
mvn -pl discovery-server spring-boot:run     # 1. wait until it is up on :8761
mvn -pl product-service  spring-boot:run     # 2.
mvn -pl order-service    spring-boot:run     # 3.
mvn -pl api-gateway      spring-boot:run     # 4.
```

Open http://localhost:8761 and check that `PRODUCT-SERVICE`, `ORDER-SERVICE` and `API-GATEWAY`
are registered (can take up to ~30 seconds after startup).

Three sample products are loaded automatically.

### Using MySQL instead of H2

```bash
export DB_USERNAME=root
export DB_PASSWORD=your_password
mvn -pl product-service spring-boot:run -Dspring-boot.run.profiles=mysql
mvn -pl order-service   spring-boot:run -Dspring-boot.run.profiles=mysql
```

The databases `product_db` and `order_db` are created automatically.

## API (call everything through the gateway on port 8080)

### Products

| Method | Path                          | Description                         |
|--------|-------------------------------|-------------------------------------|
| POST   | `/api/products`               | Create a product                    |
| GET    | `/api/products`               | List products                       |
| GET    | `/api/products/{id}`          | Get one product                     |
| PUT    | `/api/products/{id}`          | Update a product                    |
| DELETE | `/api/products/{id}`          | Delete a product                    |
| POST   | `/api/products/{id}/reserve`  | Take stock out (used by orders)     |
| POST   | `/api/products/{id}/release`  | Put stock back (used by orders)     |

### Orders

| Method | Path                       | Description                                   |
|--------|----------------------------|-----------------------------------------------|
| POST   | `/api/orders`              | Place an order                                |
| GET    | `/api/orders`              | List orders                                   |
| GET    | `/api/orders/{id}`         | Get one order                                 |
| PATCH  | `/api/orders/{id}/cancel`  | Cancel an order and return its stock          |

### Try it

```bash
# list seeded products
curl http://localhost:8080/api/products

# create a product
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Monitor","description":"27-inch 4K","price":18999.00,"stockQuantity":15}'

# place an order for 2 units of product 1
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"quantity":2}'

# product 1 stock is now 23
curl http://localhost:8080/api/products/1

# cancel the order (stock goes back to 25)
curl -X PATCH http://localhost:8080/api/orders/1/cancel

# error cases
curl -i -X POST http://localhost:8080/api/orders -H "Content-Type: application/json" \
  -d '{"productId":1,"quantity":9999}'          # 409 insufficient stock
curl -i http://localhost:8080/api/products/999   # 404 not found
curl -i -X POST http://localhost:8080/api/products -H "Content-Type: application/json" \
  -d '{"name":"","price":-1}'                    # 400 with field-level errors
```

Errors are returned as RFC 7807 `application/problem+json`.

## Design decisions (good interview talking points)

- **Layered structure**: controller -> service -> repository, with DTO records so entities never leak into the API.
- **Validation and error handling**: Bean Validation on request DTOs plus a `@RestControllerAdvice` that maps exceptions to 400 / 404 / 409 / 503.
- **Preventing overselling**: stock changes load the product with a pessimistic write lock (`SELECT ... FOR UPDATE`) inside one transaction, so two concurrent orders cannot both pass the stock check.
- **Service discovery and load balancing**: `order-service` calls `http://product-service/...`; the `@LoadBalanced` `RestClient` resolves that name through Eureka.
- **Distributed consistency**: no shared transaction across services, so a failed order save triggers a compensating `release` call, and cancelling an order releases its stock. This is a simplified version of the saga pattern.
- **Order snapshot**: the order stores the product name and unit price at purchase time, so later product edits do not rewrite history.
- **Failure handling**: if `product-service` is down, `order-service` returns `503` instead of a stack trace.

## Known limitations and ideas to extend it

- No authentication yet. Add Spring Security with JWT at the gateway.
- No circuit breaker or retry. Add Resilience4j around `ProductClient`.
- The compensating release can itself fail; a production system would use an outbox or message broker (Kafka/RabbitMQ).
- Add Dockerfiles and a `docker-compose.yml` for one-command startup.
- Add integration tests with Testcontainers, and OpenAPI docs with springdoc.

## Project structure

```
order-management-microservices/
├── pom.xml                  (parent POM, module list, dependency management)
├── discovery-server/
├── api-gateway/
├── product-service/         (model, repository, service, controller, dto, exception, config, tests)
└── order-service/           (model, repository, service, controller, client, dto, exception, config, tests)
```
