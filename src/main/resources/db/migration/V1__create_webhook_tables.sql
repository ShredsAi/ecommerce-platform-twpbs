CREATE TABLE payment_webhooks (
    id UUID PRIMARY KEY,
    processor_type VARCHAR(20) NOT NULL,
    external_event_id VARCHAR(120) NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    raw_payload JSONB NOT NULL,
    signature VARCHAR(256) NOT NULL,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    processing_status VARCHAR(15) NOT NULL DEFAULT 'PENDING',
    payment_id UUID,
    received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(processor_type, external_event_id)
);

CREATE TABLE payment_events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(20) NOT NULL,
    payment_id UUID NOT NULL,
    payment_intent_id VARCHAR(120),
    customer_id UUID NOT NULL,
    order_id UUID NOT NULL,
    amount_value NUMERIC(12,2) NOT NULL,
    amount_currency CHAR(3) NOT NULL,
    event_data JSONB,
    correlation_id VARCHAR(120) NOT NULL,
    published_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    webhook_id UUID REFERENCES payment_webhooks(id)
);

CREATE TABLE payment_webhook_correlations (
    webhook_id UUID PRIMARY KEY REFERENCES payment_webhooks(id),
    payment_id UUID NOT NULL,
    correlation_status VARCHAR(15) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE payment_status_updates (
    id BIGSERIAL PRIMARY KEY,
    payment_id UUID NOT NULL,
    old_status VARCHAR(20) NOT NULL,
    new_status VARCHAR(20) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_payment_webhooks_processor_external_event 
    ON payment_webhooks(processor_type, external_event_id);

CREATE INDEX idx_payment_events_payment_id 
    ON payment_events(payment_id);

CREATE INDEX idx_payment_status_updates_processed 
    ON payment_status_updates(processed, updated_at);