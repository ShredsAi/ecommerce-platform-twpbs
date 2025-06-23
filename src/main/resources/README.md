# Webhook Reconciliation Shred

## Overview
The Webhook Reconciliation Shred ingests payment processor webhooks (Stripe, PayPal, Square), verifies their authenticity, persists raw messages for audit, correlates them with internal payment records, updates payment state when discrepancies exist, and publishes domain events that reflect the reconciled status. The service guarantees idempotent processing, maintains a complete audit trail, and exposes inquiry APIs and scheduled reconciliation jobs.

## Architecture
The project follows Hexagonal Architecture (Ports & Adapters) pattern to maintain a clean separation between business logic and external dependencies:

- **Primary Adapters**: HTTP controllers for each payment processor and status inquiry
- **Application Layer**: Orchestrates workflow and coordinates domain services
- **Domain Layer**: Core business logic, entities, and business rules
- **Infrastructure Layer**: Database repositories, Kafka event publishing, signature verification

## API Documentation

### Webhook Endpoints
- `POST /webhooks/stripe` - Receives Stripe webhooks with `Stripe-Signature` header
- `POST /webhooks/paypal` - Receives PayPal webhooks with PayPal transmission headers
- `POST /webhooks/square` - Receives Square webhooks with `X-Square-Signature` header
- `GET /webhooks/status/{webhookId}` - Retrieves processing status of a specific webhook

### Scheduled Jobs
- `correlatePendingWebhooks()` - Runs every 30s to match previously unmatched webhooks
- `reconcileUnmatchedWebhooks()` - Runs every 5min to identify and report problematic webhooks

## Setup Instructions

### Prerequisites
- Java 17
- Docker and Docker Compose
- Maven

### Local Development
1. Clone the repository
2. Start the infrastructure services:
   ```
   docker-compose up -d
   ```
3. Build and run the application:
   ```
   mvn clean install
   mvn spring-boot:run -Dspring.profiles.active=dev
   ```

### Configuration
The application uses Spring profiles for environment-specific configuration:
- `application.yml` - Base configuration
- `application-dev.yml` - Development environment settings
- `application-prod.yml` - Production environment settings

Environment variables required in production:
- `STRIPE_WEBHOOK_SECRET` - Stripe signing secret
- `PAYPAL_CERT_URL` - PayPal certificate URL
- `SQUARE_APP_SECRET` - Square application secret
- `DB_USERNAME` - Database username
- `DB_PASSWORD` - Database password

## Database Schema

### Key Tables
- `payment_webhooks` - Stores raw webhook data and processing status
- `payment_events` - Records domain events generated from webhooks
- `payment_webhook_correlations` - Maps webhooks to internal payment records
- `payment_status_updates` - Tracks payment status changes for reconciliation