-- Create payment_intents table
CREATE TABLE payment_intents (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    amount_cents BIGINT NOT NULL CHECK (amount_cents > 0),
    currency CHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL,
    payment_method_id UUID,
    processor_type VARCHAR(16) NOT NULL,
    client_secret VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 0
);

-- Create payments table
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    payment_intent_id UUID UNIQUE NOT NULL,
    amount_cents BIGINT NOT NULL CHECK (amount_cents > 0),
    currency CHAR(3) NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('SUCCEEDED', 'FAILED')),
    processor_type VARCHAR(16) NOT NULL,
    processor_response JSONB NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 0
);

-- Create payment_methods table
CREATE TABLE payment_methods (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    type VARCHAR(16) NOT NULL CHECK (type IN ('CARD', 'BANK_ACCOUNT', 'DIGITAL_WALLET')),
    details JSONB NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create payment_tokens table
CREATE TABLE payment_tokens (
    id UUID PRIMARY KEY,
    payment_method_id UUID UNIQUE NOT NULL,
    processor_token TEXT NOT NULL,
    processor_type VARCHAR(16) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create three_d_secure table
CREATE TABLE three_d_secure (
    id UUID PRIMARY KEY,
    payment_intent_id UUID UNIQUE NOT NULL,
    challenge_url TEXT NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('PENDING', 'AUTHENTICATED', 'FAILED', 'ABANDONED')),
    authentication_result JSONB,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create payment_status_updates table
CREATE TABLE payment_status_updates (
    id BIGSERIAL PRIMARY KEY,
    payment_id UUID NOT NULL,
    intent_id UUID NOT NULL,
    status VARCHAR(16) NOT NULL,
    processor_type VARCHAR(16) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create payment_webhook_correlations table
CREATE TABLE payment_webhook_correlations (
    id BIGSERIAL PRIMARY KEY,
    webhook_id VARCHAR(128) UNIQUE NOT NULL,
    payment_id UUID,
    received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    details JSONB NOT NULL
);

-- Internal foreign key constraints (within this microservice)
ALTER TABLE payment_intents
    ADD CONSTRAINT fk_payment_intents_payment_method 
        FOREIGN KEY (payment_method_id) REFERENCES payment_methods(id);

ALTER TABLE payments
    ADD CONSTRAINT fk_payments_intent 
        FOREIGN KEY (payment_intent_id) REFERENCES payment_intents(id) 
        ON DELETE CASCADE;

ALTER TABLE payment_tokens
    ADD CONSTRAINT fk_payment_tokens_method 
        FOREIGN KEY (payment_method_id) REFERENCES payment_methods(id) 
        ON DELETE CASCADE;

ALTER TABLE three_d_secure
    ADD CONSTRAINT fk_three_d_secure_intent 
        FOREIGN KEY (payment_intent_id) REFERENCES payment_intents(id) 
        ON DELETE CASCADE;

ALTER TABLE payment_status_updates
    ADD CONSTRAINT fk_status_updates_payment 
        FOREIGN KEY (payment_id) REFERENCES payments(id) 
        ON DELETE CASCADE,
    ADD CONSTRAINT fk_status_updates_intent 
        FOREIGN KEY (intent_id) REFERENCES payment_intents(id) 
        ON DELETE CASCADE;

ALTER TABLE payment_webhook_correlations
    ADD CONSTRAINT fk_webhook_correlations_payment 
        FOREIGN KEY (payment_id) REFERENCES payments(id) 
        ON DELETE SET NULL;

-- Add constraints for status transitions
ALTER TABLE payment_intents
    ADD CONSTRAINT chk_payment_intent_status 
        CHECK (status IN ('REQUIRES_PAYMENT_METHOD', 'REQUIRES_CONFIRMATION', 'PROCESSING', 'SUCCEEDED', 'FAILED'));

ALTER TABLE payment_intents
    ADD CONSTRAINT chk_processor_type 
        CHECK (processor_type IN ('STRIPE', 'PAYPAL', 'SQUARE'));

-- Add check constraint for currency codes (ISO 4217)
ALTER TABLE payment_intents
    ADD CONSTRAINT chk_currency 
        CHECK (currency ~ '^[A-Z]{3}$');

ALTER TABLE payments
    ADD CONSTRAINT chk_payment_currency 
        CHECK (currency ~ '^[A-Z]{3}$');

-- Add updated_at trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for updated_at
CREATE TRIGGER update_payment_intents_updated_at BEFORE UPDATE ON payment_intents
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_payments_updated_at BEFORE UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_payment_methods_updated_at BEFORE UPDATE ON payment_methods
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_payment_tokens_updated_at BEFORE UPDATE ON payment_tokens
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_three_d_secure_updated_at BEFORE UPDATE ON three_d_secure
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();