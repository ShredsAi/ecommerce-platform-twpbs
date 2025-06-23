-- Foreign key indexes for improved join performance
CREATE INDEX idx_payment_intents_order_id ON payment_intents(order_id);
CREATE INDEX idx_payment_intents_customer_id ON payment_intents(customer_id);
CREATE INDEX idx_payment_intents_payment_method_id ON payment_intents(payment_method_id);

-- Already have unique constraint on payment_intent_id in payments table
-- Adding additional indexes for common query patterns
CREATE INDEX idx_payment_methods_customer_id ON payment_methods(customer_id);

-- Indexed for efficient lookups on 3D secure authentication
CREATE INDEX idx_three_d_secure_payment_intent_id ON three_d_secure(payment_intent_id);

-- Composite index for status updates to efficiently find latest updates for a payment
CREATE INDEX idx_status_updates_payment_id_updated_at ON payment_status_updates(payment_id, updated_at DESC);
CREATE INDEX idx_status_updates_intent_id_updated_at ON payment_status_updates(intent_id, updated_at DESC);

-- Special performance indexes for business operations
-- Find expired intents efficiently
CREATE INDEX idx_payment_intents_expires_at ON payment_intents(expires_at);
CREATE INDEX idx_payment_intents_status ON payment_intents(status);

-- Find unprocessed webhooks efficiently
CREATE INDEX idx_payment_webhook_correlations_processed ON payment_webhook_correlations(processed) WHERE processed = FALSE;

-- Webhook correlation lookups
CREATE INDEX idx_payment_webhook_correlations_payment_id ON payment_webhook_correlations(payment_id);

-- Composite index for finding intents within a time range
CREATE INDEX idx_payment_intents_created_at ON payment_intents(created_at);
CREATE INDEX idx_payments_processed_at ON payments(processed_at);

-- Multi-column index for reporting queries
CREATE INDEX idx_payments_status_processed_at ON payments(status, processed_at);

-- For efficient search by processor type
CREATE INDEX idx_payment_intents_processor_type ON payment_intents(processor_type);
CREATE INDEX idx_payments_processor_type ON payments(processor_type);

-- Partial index for active payment methods
CREATE INDEX idx_payment_methods_active ON payment_methods(customer_id) WHERE is_active = TRUE;

-- Index for default payment methods
CREATE INDEX idx_payment_methods_default ON payment_methods(customer_id) WHERE is_default = TRUE;

-- Index for payment tokens expiry management
CREATE INDEX idx_payment_tokens_expires_at ON payment_tokens(expires_at);
