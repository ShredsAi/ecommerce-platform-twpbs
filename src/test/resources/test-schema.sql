-- Test Database Schema for Inventory Tracking Integration Tests

-- SKU Table
CREATE TABLE IF NOT EXISTS sku (
    sku_id VARCHAR(64) PRIMARY KEY,
    product_id VARCHAR(64),
    vendor_sku VARCHAR(64),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Location Table
CREATE TABLE IF NOT EXISTS location (
    location_id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    type VARCHAR(32) NOT NULL CHECK (type IN ('WAREHOUSE', 'STORE', 'FULFILLMENT_CENTER')),
    address TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Stock Ledger Table
CREATE TABLE IF NOT EXISTS stock_ledger (
    ledger_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sku_id VARCHAR(64) NOT NULL REFERENCES sku(sku_id) ON DELETE RESTRICT,
    location_id VARCHAR(64) NOT NULL REFERENCES location(location_id) ON DELETE RESTRICT,
    quantity NUMERIC(19,4) NOT NULL CHECK (quantity >= 0),
    reserved NUMERIC(19,4) NOT NULL DEFAULT 0 CHECK (reserved >= 0),
    available NUMERIC(19,4) GENERATED ALWAYS AS (quantity - reserved) STORED,
    last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 0,
    CONSTRAINT unique_sku_location UNIQUE (sku_id, location_id)
);

-- Safety Stock Rule Table
CREATE TABLE IF NOT EXISTS safety_stock_rule (
    rule_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sku_id VARCHAR(64) NOT NULL REFERENCES sku(sku_id) ON DELETE CASCADE,
    location_id VARCHAR(64) NOT NULL REFERENCES location(location_id) ON DELETE CASCADE,
    min_quantity NUMERIC(19,4) NOT NULL CHECK (min_quantity >= 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_safety_rule UNIQUE (sku_id, location_id)
);

-- Low Stock Alert Table
CREATE TABLE IF NOT EXISTS low_stock_alert (
    alert_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sku_id VARCHAR(64) NOT NULL REFERENCES sku(sku_id) ON DELETE CASCADE,
    location_id VARCHAR(64) NOT NULL REFERENCES location(location_id) ON DELETE CASCADE,
    rule_id UUID REFERENCES safety_stock_rule(rule_id) ON DELETE SET NULL,
    current_quantity NUMERIC(19,4) NOT NULL,
    threshold NUMERIC(19,4) NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('PENDING', 'ACKNOWLEDGED', 'RESOLVED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    acknowledged_at TIMESTAMPTZ,
    resolved_at TIMESTAMPTZ
);

-- Reservation Table
CREATE TABLE IF NOT EXISTS reservation (
    reservation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sku_id VARCHAR(64) NOT NULL REFERENCES sku(sku_id) ON DELETE CASCADE,
    location_id VARCHAR(64) NOT NULL REFERENCES location(location_id) ON DELETE CASCADE,
    quantity NUMERIC(19,4) NOT NULL CHECK (quantity > 0),
    status VARCHAR(32) NOT NULL CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'EXPIRED')),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reason VARCHAR(255)
);

-- Stock Adjustment Audit Table
CREATE TABLE IF NOT EXISTS stock_adjustment_audit (
    audit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ledger_id UUID NOT NULL REFERENCES stock_ledger(ledger_id) ON DELETE CASCADE,
    sku_id VARCHAR(64) NOT NULL,
    location_id VARCHAR(64) NOT NULL,
    delta_quantity NUMERIC(19,4) NOT NULL,
    previous_quantity NUMERIC(19,4) NOT NULL,
    new_quantity NUMERIC(19,4) NOT NULL,
    reason VARCHAR(32) NOT NULL CHECK (reason IN ('STOCK_RECEIPT', 'DAMAGE', 'CYCLE_COUNT', 'ERP_SYNC', 'RETURN', 'TRANSFER', 'THEFT', 'OTHER')),
    source VARCHAR(32) NOT NULL DEFAULT 'INTERNAL',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    user_id VARCHAR(64)
);

-- ERP Reconciliation Table
CREATE TABLE IF NOT EXISTS erp_reconciliation (
    reconciliation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED')),
    processed_at TIMESTAMPTZ,
    total_records INT NOT NULL,
    success_count INT NOT NULL DEFAULT 0,
    error_count INT NOT NULL DEFAULT 0,
    errors JSONB
);

-- Outbox Event Table
CREATE TABLE IF NOT EXISTS outbox_event (
    event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id UUID NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload JSONB NOT NULL,
    occurred_on TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    processed_on TIMESTAMPTZ
);

-- Indexes for better test performance
CREATE INDEX IF NOT EXISTS idx_stock_ledger_sku_location ON stock_ledger(sku_id, location_id);
CREATE INDEX IF NOT EXISTS idx_outbox_event_processed ON outbox_event(processed) WHERE NOT processed;
CREATE INDEX IF NOT EXISTS idx_alert_status ON low_stock_alert(status) WHERE status != 'RESOLVED';
CREATE INDEX IF NOT EXISTS idx_reservation_expiry ON reservation(expires_at) WHERE status = 'PENDING';

-- Insert test data for integration tests
INSERT INTO sku (sku_id, product_id, vendor_sku, is_active) VALUES
('TEST-SKU-001', 'PROD-001', 'VENDOR-SKU-001', true),
('TEST-SKU-002', 'PROD-002', 'VENDOR-SKU-002', true),
('TEST-SKU-003', 'PROD-003', 'VENDOR-SKU-003', false)
ON CONFLICT (sku_id) DO NOTHING;

INSERT INTO location (location_id, name, type, address, is_active) VALUES
('TEST-LOC-001', 'Test Warehouse 1', 'WAREHOUSE', '123 Test Street, Test City', true),
('TEST-LOC-002', 'Test Store 1', 'STORE', '456 Store Avenue, Store City', true),
('TEST-LOC-003', 'Test Fulfillment Center', 'FULFILLMENT_CENTER', '789 FC Road, FC Town', true)
ON CONFLICT (location_id) DO NOTHING;

INSERT INTO stock_ledger (sku_id, location_id, quantity, reserved) VALUES
('TEST-SKU-001', 'TEST-LOC-001', 100.0000, 10.0000),
('TEST-SKU-002', 'TEST-LOC-001', 50.0000, 5.0000),
('TEST-SKU-001', 'TEST-LOC-002', 25.0000, 0.0000)
ON CONFLICT (sku_id, location_id) DO NOTHING;

INSERT INTO safety_stock_rule (sku_id, location_id, min_quantity, is_active) VALUES
('TEST-SKU-001', 'TEST-LOC-001', 20.0000, true),
('TEST-SKU-002', 'TEST-LOC-001', 10.0000, true)
ON CONFLICT (sku_id, location_id) DO NOTHING;
