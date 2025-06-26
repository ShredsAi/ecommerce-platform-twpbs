# Order Cancellation and Returns Service

## Architecture Overview

The Order Cancellation and Returns Service orchestrates the full life-cycle of order cancellations and product returns. It validates eligibility, manages refunds via the Payment Service, coordinates inventory adjustments, and publishes domain events to Kafka, JMS, and Spring Application Events. It serves as the single source of truth for cancellation and return status, ensuring eventual consistency across the order-management ecosystem.

## Prerequisites

- Java 17 (Eclipse Temurin or equivalent)
- Maven 3.9+
- Docker & Docker Compose
- PostgreSQL 15
- Apache Kafka 7.5.0
- Zookeeper 7.5.0
- ActiveMQ 5.18.3

## Setup Instructions

1. Clone the repository:
   ```bash
   git clone https://github.com/your-org/order-cancellation-returns.git
   cd order-cancellation-returns
   ```
2. Configure environment variables or update `application.yml` for database, Kafka, JMS, and external service URLs.
3. Build and package the application:
   ```bash
   mvn clean package -DskipTests
   ```
4. Start dependencies via Docker Compose:
   ```bash
   docker-compose -f src/files/docker-compose.yml up -d
   ```
5. Run the application:
   ```bash
   java -jar target/order-cancellation-returns-0.0.1-SNAPSHOT.jar
   ```

## API Documentation

Swagger UI is available at `http://localhost:8080/swagger-ui.html` once the application is running. Endpoints include:

- `POST /api/cancellations` – Request a cancellation
- `GET /api/cancellations/{id}` – Get cancellation details
- `POST /api/returns` – Request a return
- `GET /api/returns/{id}` – Get return details

## Configuration Guide

Application properties can be found in `src/main/resources/application.yml`. Profiles:

- `application-dev.yml` – Development settings (show SQL, debug logging)
- `application-prod.yml` – Production settings (externalized URLs, health endpoints)

Key configuration entries:

- Spring Data JPA datasource settings
- Kafka and JMS broker URLs
- gRPC service hosts and ports
- Resilience4j retry and circuit breaker instances
- Scheduler cron expressions for time-based tasks

## Business Rules

- Cancellations allowed until 2 hours before dispatch or until SHIPPED status.
- Returns must be requested within 30 days of delivery (45 days during holidays).
- Duplicate or overlapping cancellation/return requests are rejected.
- Refund calculated minus restocking fees; shipping refunded only on seller faults.

## Testing Guide

- Unit tests: `mvn test`
- Integration tests with in-memory database and embedded Kafka/ActiveMQ:
  ```bash
  mvn verify -P integration-tests
  ```

## Deployment Instructions

Use Docker and Docker Compose for local deployments. For Kubernetes or other platforms, build Docker image and push to registry:

```bash
docker build -t your-registry/order-cancellation-returns:latest .
docker push your-registry/order-cancellation-returns:latest
```

Apply your platform's deployment manifests or Helm charts.
