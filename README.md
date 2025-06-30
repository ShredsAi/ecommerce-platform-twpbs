# Inventory Tracking Shred

## Overview
The Inventory Tracking Shred acts as the single source of truth for all stock quantities across the entire platform. It maintains the StockLedger for every SKU-Location pair, calculates real-time available quantities, enforces safety-stock policies, and generates low-stock alerts whenever thresholds are breached.

## Architecture
This application follows a Hexagonal (Ports & Adapters) architecture:
- Primary Adapters: REST controllers, JMS listeners, Kafka producers
- Application Core: Command & Query services, domain aggregates, event factories
- Persistence Adapters: JPA repositories, Outbox pattern
- External Services: Redis cache, Kafka cluster, JMS broker

## Prerequisites
- Java 17
- Maven 3.8+
- Docker & Docker Compose
- PostgreSQL, Redis, Kafka, ActiveMQ (via Docker Compose)

## Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/your-org/inventory-tracking-shred.git
   cd inventory-tracking-shred
   ```
2. Build the project:
   ```bash
   mvn clean package -DskipTests
   ```
3. Run with Docker Compose:
   ```bash
   docker-compose up --build
   ```

## Configuration
Configuration files are located in `src/main/resources`:
- `application.yml` (default)
- `application-dev.yml` (development)
- `application-prod.yml` (production)

## API Documentation
OpenAPI spec is available at `src/main/resources/api/openapi.yaml` and exposed at `/v3/api-docs` when the application is running.

## Testing
Run unit and integration tests:
```bash
mvn test
```

## Deployment
Deploy the Docker image to your container registry and run in your Kubernetes or Docker Swarm environment. Ensure environment variables are set for production profiles.
