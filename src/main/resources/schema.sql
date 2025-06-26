-- Order Fulfillment Coordination Database Schema
-- PostgreSQL Database Schema for Order Fulfillment Coordination System

-- Create extensions if not exists
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Drop tables if they exist (for clean deployments)
DROP TABLE IF EXISTS order_events CASCADE;
DROP TABLE IF EXISTS saga_state CASCADE;
DROP TABLE IF EXISTS payment_details CASCADE;
DROP TABLE IF EXISTS shipping_details CASCADE;
DROP TABLE IF EXISTS order_items CASCADE;
DROP TABLE IF EXISTS orders CASCADE;

-- Create orders table
CREATE TABLE orders (
    order_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id VARCHAR(64) NOT NULL,
    order_number VARCHAR(64) UNIQUE NOT NULL,
    order_status VARCHAR(32) NOT NULL,
    order_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    total_amount NUMERIC(12,2) NOT NULL,
    subtotal_amount NUMERIC(12,2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'USD',
    billing_street1 VARCHAR(255),
    billing_street2 VARCHAR(255),
    billing_city VARCHAR(100),
    billing_state VARCHAR(100),
    billing_postal_code VARCHAR(20),
    billing_country VARCHAR(100),
    shipping_street1 VARCHAR(255),
    shipping_street2 VARCHAR(255),
    shipping_city VARCHAR(100),
    shipping_state VARCHAR(100),
    shipping_postal_code VARCHAR(20),
    shipping_country VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 0
);

-- Create order_items table
CREATE TABLE order_items (
    order_item_id VARCHAR(64) PRIMARY KEY,
    order_id UUID NOT NULL,
    product_id VARCHAR(64) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(12,2) NOT NULL,
    total_price NUMERIC(12,2) NOT NULL,
    item_status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE
);

-- Create payment_details table
CREATE TABLE payment_details (
    order_id UUID PRIMARY KEY,
    payment_status VARCHAR(32) NOT NULL,
    transaction_id VARCHAR(128),
    authorization_code VARCHAR(64),
    amount NUMERIC(12,2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'USD',
    payment_method VARCHAR(32),
    gateway_response_code VARCHAR(32),
    gateway_response_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE
);

-- Create shipping_details table
CREATE TABLE shipping_details (
    order_id UUID PRIMARY KEY,
    shipping_status VARCHAR(32) NOT NULL,
    tracking_number VARCHAR(64),
    carrier VARCHAR(64),
    shipping_method VARCHAR(64),
    estimated_delivery_date DATE,
    actual_delivery_date DATE,
    shipping_cost NUMERIC(12,2),
    expedited BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE
);

-- Create order_events table for audit trail
CREATE TABLE order_events (
    event_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    event_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    event_data JSONB,
    previous_status VARCHAR(32),
    new_status VARCHAR(32),
    correlation_id VARCHAR(64),
    saga_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE
);

-- Create saga_state table for orchestration
CREATE TABLE saga_state (
    saga_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL,
    current_step VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_activity TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    timeout_threshold INTERVAL NOT NULL DEFAULT INTERVAL '5 minutes',
    next_retry TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    compensation_required BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE
);

-- Create indexes for performance optimization

-- Orders indexes
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_order_status ON orders(order_status);
CREATE INDEX idx_orders_order_date ON orders(order_date);
CREATE INDEX idx_orders_created_at ON orders(created_at);
CREATE INDEX idx_orders_updated_at ON orders(updated_at);

-- Order items indexes
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);
CREATE INDEX idx_order_items_status ON order_items(item_status);

-- Payment details indexes
CREATE INDEX idx_payment_details_status ON payment_details(payment_status);
CREATE INDEX idx_payment_details_transaction_id ON payment_details(transaction_id);
CREATE INDEX idx_payment_details_created_at ON payment_details(created_at);

-- Shipping details indexes
CREATE INDEX idx_shipping_details_status ON shipping_details(shipping_status);
CREATE INDEX idx_shipping_details_tracking_number ON shipping_details(tracking_number);
CREATE INDEX idx_shipping_details_carrier ON shipping_details(carrier);
CREATE INDEX idx_shipping_details_estimated_delivery ON shipping_details(estimated_delivery_date);

-- Order events indexes
CREATE INDEX idx_order_events_order_id ON order_events(order_id);
CREATE INDEX idx_order_events_event_type ON order_events(event_type);
CREATE INDEX idx_order_events_event_timestamp ON order_events(event_timestamp DESC);
CREATE INDEX idx_order_events_correlation_id ON order_events(correlation_id);
CREATE INDEX idx_order_events_saga_id ON order_events(saga_id);
CREATE INDEX idx_order_events_order_id_timestamp ON order_events(order_id, event_timestamp DESC);

-- Saga state indexes
CREATE INDEX idx_saga_state_order_id ON saga_state(order_id);
CREATE INDEX idx_saga_state_status ON saga_state(status);
CREATE INDEX idx_saga_state_current_step ON saga_state(current_step);
CREATE INDEX idx_saga_state_last_activity ON saga_state(last_activity);
CREATE INDEX idx_saga_state_timeout_scan ON saga_state(status, last_activity) WHERE status = 'IN_PROGRESS';
CREATE INDEX idx_saga_state_next_retry ON saga_state(next_retry) WHERE next_retry IS NOT NULL;

-- Create triggers for automatic timestamp updates
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply timestamp triggers to relevant tables
CREATE TRIGGER update_orders_updated_at BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_order_items_updated_at BEFORE UPDATE ON order_items
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_payment_details_updated_at BEFORE UPDATE ON payment_details
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_shipping_details_updated_at BEFORE UPDATE ON shipping_details
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_saga_state_updated_at BEFORE UPDATE ON saga_state
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create constraints
ALTER TABLE orders ADD CONSTRAINT chk_orders_total_amount_positive CHECK (total_amount >= 0);
ALTER TABLE orders ADD CONSTRAINT chk_orders_subtotal_amount_positive CHECK (subtotal_amount >= 0);
ALTER TABLE orders ADD CONSTRAINT chk_orders_status_valid CHECK (order_status IN ('PENDING', 'CONFIRMED', 'PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'COMPLETED', 'CANCELLED'));

ALTER TABLE order_items ADD CONSTRAINT chk_order_items_quantity_positive CHECK (quantity > 0);
ALTER TABLE order_items ADD CONSTRAINT chk_order_items_unit_price_positive CHECK (unit_price >= 0);
ALTER TABLE order_items ADD CONSTRAINT chk_order_items_total_price_positive CHECK (total_price >= 0);
ALTER TABLE order_items ADD CONSTRAINT chk_order_items_status_valid CHECK (item_status IN ('PENDING', 'CONFIRMED', 'ALLOCATED', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'RETURNED'));

ALTER TABLE payment_details ADD CONSTRAINT chk_payment_details_amount_positive CHECK (amount >= 0);
ALTER TABLE payment_details ADD CONSTRAINT chk_payment_details_status_valid CHECK (payment_status IN ('INITIATED', 'AUTHORIZED', 'CAPTURED', 'FAILED', 'REFUNDED'));

ALTER TABLE shipping_details ADD CONSTRAINT chk_shipping_details_status_valid CHECK (shipping_status IN ('PENDING', 'ARRANGED', 'SHIPPED', 'DELIVERED', 'FAILED'));

ALTER TABLE order_events ADD CONSTRAINT chk_order_events_event_type_valid CHECK (event_type IN ('ORDER_CREATED', 'PAYMENT_INITIATED', 'PAYMENT_AUTHORIZED', 'PAYMENT_CAPTURED', 'PAYMENT_FAILED', 'SHIPPING_ARRANGED', 'SHIPPING_FAILED', 'ORDER_SHIPPED', 'ORDER_DELIVERED', 'ORDER_CANCELLED', 'TIMEOUT_HANDLED', 'SAGA_TIMEOUT_EXHAUSTED'));

ALTER TABLE saga_state ADD CONSTRAINT chk_saga_state_retry_count_non_negative CHECK (retry_count >= 0);
ALTER TABLE saga_state ADD CONSTRAINT chk_saga_state_status_valid CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'COMPENSATING', 'FAILED', 'TIMED_OUT'));
ALTER TABLE saga_state ADD CONSTRAINT chk_saga_state_current_step_valid CHECK (current_step IN ('PAYMENT_AUTHORIZATION', 'PAYMENT_CAPTURE', 'SHIPPING_ARRANGEMENT', 'INVENTORY_ALLOCATION', 'NOTIFICATION_SENDING', 'COMPENSATION_PAYMENT', 'COMPENSATION_SHIPPING', 'COMPENSATION_INVENTORY'));

-- Create unique constraints
ALTER TABLE saga_state ADD CONSTRAINT uk_saga_state_order_id UNIQUE (order_id);

-- Insert sample data for testing (optional)
-- INSERT INTO orders (order_id, customer_id, order_number, order_status, total_amount, subtotal_amount)
-- VALUES ('550e8400-e29b-41d4-a716-446655440000', 'CUST-001', 'ORD-001', 'PENDING', 99.99, 89.99);

-- Create views for common queries
CREATE OR REPLACE VIEW v_order_summary AS
SELECT 
    o.order_id,
    o.order_number,
    o.customer_id,
    o.order_status,
    o.total_amount,
    o.currency,
    o.created_at,
    o.updated_at,
    pd.payment_status,
    pd.transaction_id,
    sd.shipping_status,
    sd.tracking_number,
    sd.carrier,
    ss.saga_id,
    ss.current_step AS saga_current_step,
    ss.status AS saga_status,
    ss.retry_count AS saga_retry_count
FROM orders o
LEFT JOIN payment_details pd ON o.order_id = pd.order_id
LEFT JOIN shipping_details sd ON o.order_id = sd.order_id
LEFT JOIN saga_state ss ON o.order_id = ss.order_id;

CREATE OR REPLACE VIEW v_saga_timeout_candidates AS
SELECT 
    ss.saga_id,
    ss.order_id,
    ss.current_step,
    ss.status,
    ss.retry_count,
    ss.last_activity,
    ss.timeout_threshold,
    ss.next_retry,
    NOW() - ss.last_activity AS inactive_duration,
    CASE 
        WHEN ss.next_retry IS NOT NULL AND NOW() >= ss.next_retry THEN true
        WHEN NOW() - ss.last_activity > ss.timeout_threshold THEN true
        ELSE false
    END AS should_timeout
FROM saga_state ss
WHERE ss.status = 'IN_PROGRESS'
ORDER BY ss.last_activity ASC;

-- Grant permissions (adjust as needed for your environment)
-- GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO order_fulfillment_app;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO order_fulfillment_app;

-- Comments for documentation
COMMENT ON TABLE orders IS 'Main orders table containing order header information';
COMMENT ON TABLE order_items IS 'Order line items with product details and quantities';
COMMENT ON TABLE payment_details IS 'Payment processing details and transaction information';
COMMENT ON TABLE shipping_details IS 'Shipping and logistics tracking information';
COMMENT ON TABLE order_events IS 'Audit trail of all order state changes and events';
COMMENT ON TABLE saga_state IS 'Saga orchestration state for distributed transaction management';

COMMENT ON COLUMN orders.version IS 'Optimistic locking version field';
COMMENT ON COLUMN saga_state.version IS 'Optimistic locking version field';
COMMENT ON COLUMN saga_state.timeout_threshold IS 'Maximum allowed inactivity before timeout triggers';
COMMENT ON COLUMN order_events.event_data IS 'JSON payload containing event-specific data';

-- Analyze tables for query optimization
ANALYZE orders;
ANALYZE order_items;
ANALYZE payment_details;
ANALYZE shipping_details;
ANALYZE order_events;
ANALYZE saga_state;