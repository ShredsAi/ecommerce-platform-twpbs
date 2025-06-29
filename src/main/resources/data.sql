-- Sample data for Inventory Tracking Shred

-- Insert sample SKUs
INSERT INTO sku (sku_id, product_id, vendor_sku, is_active, created_at, updated_at) 
VALUES 
('SKU-001', 'PROD-001', 'VSKU-001', true, NOW(), NOW()),
('SKU-002', 'PROD-002', 'VSKU-002', true, NOW(), NOW()),
('SKU-003', 'PROD-003', 'VSKU-003', true, NOW(), NOW()),
('SKU-004', 'PROD-004', 'VSKU-004', true, NOW(), NOW()),
('SKU-005', 'PROD-005', 'VSKU-005', false, NOW(), NOW()),
('SKU-006', 'PROD-006', 'VSKU-006', true, NOW(), NOW()),
('SKU-007', 'PROD-007', 'VSKU-007', true, NOW(), NOW()),
('SKU-008', 'PROD-008', 'VSKU-008', true, NOW(), NOW()),
('SKU-009', 'PROD-009', 'VSKU-009', true, NOW(), NOW()),
('SKU-010', 'PROD-010', 'VSKU-010', true, NOW(), NOW());

-- Insert sample Locations
INSERT INTO location (location_id, name, type, address, is_active, created_at) 
VALUES 
('LOC-001', 'Main Warehouse', 'WAREHOUSE', '{"street": "123 Main St", "city": "New York", "state": "NY", "postalCode": "10001", "country": "US"}', true, NOW()),
('LOC-002', 'Downtown Store', 'STORE', '{"street": "456 Elm St", "city": "New York", "state": "NY", "postalCode": "10002", "country": "US"}', true, NOW()),
('LOC-003', 'West Coast FC', 'FULFILLMENT_CENTER', '{"street": "789 Oak Ave", "city": "Los Angeles", "state": "CA", "postalCode": "90001", "country": "US"}', true, NOW()),
('LOC-004', 'East Coast FC', 'FULFILLMENT_CENTER', '{"street": "321 Pine Rd", "city": "Miami", "state": "FL", "postalCode": "33101", "country": "US"}', true, NOW()),
('LOC-005', 'Midwest Store', 'STORE', '{"street": "555 Maple Dr", "city": "Chicago", "state": "IL", "postalCode": "60601", "country": "US"}', false, NOW()),
('LOC-006', 'Canada Warehouse', 'WAREHOUSE', '{"street": "123 Maple Ave", "city": "Toronto", "state": "ON", "postalCode": "M5V 2L7", "country": "CA"}', true, NOW());

-- Insert stock ledger entries
INSERT INTO stock_ledger (ledger_id, sku_id, location_id, quantity, reserved, last_updated, version) 
VALUES 
('5a1c0f3e-b7f3-4f4a-8ab1-dc6a8b7a81a1'::UUID, 'SKU-001', 'LOC-001', 100.0000, 10.0000, NOW(), 0),
('5a1c0f3e-b7f3-4f4a-8ab1-dc6a8b7a81a2'::UUID, 'SKU-001', 'LOC-002', 50.0000, 5.0000, NOW(), 0),
('5a1c0f3e-b7f3-4f4a-8ab1-dc6a8b7a81a3'::UUID, 'SKU-001', 'LOC-003', 200.0000, 0.0000, NOW(), 0),
('5a1c0f3e-b7f3-4f4a-8ab1-dc6a8b7a81a4'::UUID, 'SKU-002', 'LOC-001', 75.0000, 25.0000, NOW(), 0),
('5a1c0f3e-b7f3-4f4a-8ab1-dc6a8b7a81a5'::UUID, 'SKU-002', 'LOC-002', 30.0000, 0.0000, NOW(), 0),
('5a1c0f3e-b7f3-4f4a-8ab1-dc6a8b7a81a6'::UUID, 'SKU-003', 'LOC-001', 150.0000, 50.0000, NOW(), 0),
('5a1c0f3e-b7f3-4f4a-8ab1-dc6a8b7a81a7'::UUID, 'SKU-004', 'LOC-003', 300.0000, 100.0000, NOW(), 0),
('5a1c0f3e-b7f3-4f4a-8ab1-dc6a8b7a81a8'::UUID, 'SKU-005', 'LOC-004', 25.0000, 0.0000, NOW(), 0),
('5a1c0f3e-b7f3-4f4a-8ab1-dc6a8b7a81a9'::UUID, 'SKU-006', 'LOC-001', 10.0000, 5.0000, NOW(), 0),
('5a1c0f3e-b7f3-4f4a-8ab1-dc6a8b7a81b0'::UUID, 'SKU-007', 'LOC-001', 5.0000, 0.0000, NOW(), 0);

-- Insert sample safety stock rules
INSERT INTO safety_stock_rule (rule_id, sku_id, location_id, min_quantity, is_active, created_at, updated_at) 
VALUES
('6b2d1g4f-c8g4-5g5b-9bc2-ed7b9c8b92b1'::UUID, 'SKU-001', 'LOC-001', 20.0000, true, NOW(), NOW()),
('6b2d1g4f-c8g4-5g5b-9bc2-ed7b9c8b92b2'::UUID, 'SKU-001', 'LOC-002', 10.0000, true, NOW(), NOW()),
('6b2d1g4f-c8g4-5g5b-9bc2-ed7b9c8b92b3'::UUID, 'SKU-002', 'LOC-001', 15.0000, true, NOW(), NOW()),
('6b2d1g4f-c8g4-5g5b-9bc2-ed7b9c8b92b4'::UUID, 'SKU-003', 'LOC-001', 30.0000, true, NOW(), NOW()),
('6b2d1g4f-c8g4-5g5b-9bc2-ed7b9c8b92b5'::UUID, 'SKU-006', 'LOC-001', 20.0000, true, NOW(), NOW()),
('6b2d1g4f-c8g4-5g5b-9bc2-ed7b9c8b92b6'::UUID, 'SKU-007', 'LOC-001', 10.0000, true, NOW(), NOW());

-- Insert sample low stock alerts (one in each state)
INSERT INTO low_stock_alert (alert_id, sku_id, location_id, rule_id, current_quantity, threshold, status, created_at, acknowledged_at, resolved_at) 
VALUES
('7c3e2h5g-d9h5-6h6c-0cd3-fe8c0d9c03c1'::UUID, 'SKU-006', 'LOC-001', '6b2d1g4f-c8g4-5g5b-9bc2-ed7b9c8b92b5'::UUID, 10.0000, 20.0000, 'PENDING', NOW() - INTERVAL '3 hour', NULL, NULL),
('7c3e2h5g-d9h5-6h6c-0cd3-fe8c0d9c03c2'::UUID, 'SKU-007', 'LOC-001', '6b2d1g4f-c8g4-5g5b-9bc2-ed7b9c8b92b6'::UUID, 5.0000, 10.0000, 'ACKNOWLEDGED', NOW() - INTERVAL '1 day', NOW() - INTERVAL '23 hour', NULL),
('7c3e2h5g-d9h5-6h6c-0cd3-fe8c0d9c03c3'::UUID, 'SKU-001', 'LOC-002', '6b2d1g4f-c8g4-5g5b-9bc2-ed7b9c8b92b2'::UUID, 8.0000, 10.0000, 'RESOLVED', NOW() - INTERVAL '2 day', NOW() - INTERVAL '47 hour', NOW() - INTERVAL '23 hour');

-- Insert sample Reservations
INSERT INTO reservation (reservation_id, sku_id, location_id, quantity, status, expires_at, created_at, reason) 
VALUES
('8d4f3i6h-e0i6-7i7d-1de4-gf9d1e0d14d1'::UUID, 'SKU-001', 'LOC-001', 10.0000, 'PENDING', NOW() + INTERVAL '24 hour', NOW(), 'Order #12345'),
('8d4f3i6h-e0i6-7i7d-1de4-gf9d1e0d14d2'::UUID, 'SKU-002', 'LOC-001', 25.0000, 'CONFIRMED', NOW() + INTERVAL '12 hour', NOW() - INTERVAL '12 hour', 'Order #12346'),
('8d4f3i6h-e0i6-7i7d-1de4-gf9d1e0d14d3'::UUID, 'SKU-003', 'LOC-001', 50.0000, 'PENDING', NOW() + INTERVAL '24 hour', NOW(), 'Order #12347'),
('8d4f3i6h-e0i6-7i7d-1de4-gf9d1e0d14d4'::UUID, 'SKU-004', 'LOC-003', 100.0000, 'PENDING', NOW() + INTERVAL '24 hour', NOW(), 'Order #12348'),
('8d4f3i6h-e0i6-7i7d-1de4-gf9d1e0d14d5'::UUID, 'SKU-001', 'LOC-002', 5.0000, 'PENDING', NOW() + INTERVAL '24 hour', NOW(), 'Order #12349'),
('8d4f3i6h-e0i6-7i7d-1de4-gf9d1e0d14d6'::UUID, 'SKU-006', 'LOC-001', 5.0000, 'CANCELLED', NOW() - INTERVAL '1 hour', NOW() - INTERVAL '3 hour', 'Order #12350 Cancelled');

-- Insert sample stock adjustment audits
INSERT INTO stock_adjustment_audit (audit_id, ledger_id, sku_id, location_id, delta_quantity, previous_quantity, new_quantity, reason, source, created_at, user_id) 
VALUES
('9e5g4j7i-f1j7-8j8e-2ef5-hg0e2f1e25e1'::UUID, '5a1c0f3e-b7f3-4f4a-8ab1-dc6a8b7a81a1'::UUID, 'SKU-001', 'LOC-001', 20.0000, 80.0000, 100.0000, 'STOCK_RECEIPT', 'INTERNAL', NOW() - INTERVAL '1 day', 'user123'),
('9e5g4j7i-f1j7-8j8e-2ef5-hg0e2f1e25e2'::UUID, '5a1c0f3e-b7f3-4f4a-8ab1-dc6a8b7a81a2'::UUID, 'SKU-001', 'LOC-002', 10.0000, 40.0000, 50.0000, 'STOCK_RECEIPT', 'INTERNAL', NOW() - INTERVAL '2 day', 'user123'),
('9e5g4j7i-f1j7-8j8e-2ef5-hg0e2f1e25e3'::UUID, '5a1c0f3e-b7f3-4f4a-8ab1-dc6a8b7a81a6'::UUID, 'SKU-003', 'LOC-001', -5.0000, 155.0000, 150.0000, 'DAMAGE', 'INTERNAL', NOW() - INTERVAL '3 day', 'user456'),
('9e5g4j7i-f1j7-8j8e-2ef5-hg0e2f1e25e4'::UUID, '5a1c0f3e-b7f3-4f4a-8ab1-dc6a8b7a81a4'::UUID, 'SKU-002', 'LOC-001', 25.0000, 50.0000, 75.0000, 'CYCLE_COUNT', 'INTERNAL', NOW() - INTERVAL '4 day', 'user789'),
('9e5g4j7i-f1j7-8j8e-2ef5-hg0e2f1e25e5'::UUID, '5a1c0f3e-b7f3-4f4a-8ab1-dc6a8b7a81a7'::UUID, 'SKU-004', 'LOC-003', 50.0000, 250.0000, 300.0000, 'RETURN', 'INTERNAL', NOW() - INTERVAL '5 day', 'user123');

-- Insert ERP Reconciliation record
INSERT INTO erp_reconciliation (reconciliation_id, batch_id, status, processed_at, total_records, success_count, error_count, errors) 
VALUES
('0f6h5k8j-g2k8-9k9f-3fg6-ih1f3g2f36f1'::UUID, 'ERP-BATCH-001', 'COMPLETED', NOW() - INTERVAL '6 hour', 10, 10, 0, NULL),
('0f6h5k8j-g2k8-9k9f-3fg6-ih1f3g2f36f2'::UUID, 'ERP-BATCH-002', 'FAILED', NOW() - INTERVAL '12 hour', 5, 3, 2, '{"errors": [{"skuId": "SKU-999", "locationId": "LOC-001", "errorMessage": "SKU not found", "errorCode": "NOT_FOUND"}, {"skuId": "SKU-001", "locationId": "LOC-999", "errorMessage": "Location not found", "errorCode": "NOT_FOUND"}]}');

-- Insert sample outbox events
INSERT INTO outbox_event (event_id, aggregate_id, aggregate_type, event_type, payload, occurred_on, processed, processed_on) 
VALUES
('1g7i6l9k-h3l9-0l0g-4gh7-ji2g4h3g47g1'::UUID, '5a1c0f3e-b7f3-4f4a-8ab1-dc6a8b7a81a1'::UUID, 'STOCK_LEDGER', 'STOCK_LEVEL_CHANGED', '{"skuId": "SKU-001", "locationId": "LOC-001", "previousQuantity": 80, "newQuantity": 100, "source": "INTERNAL"}', NOW() - INTERVAL '1 day', true, NOW() - INTERVAL '1 day'),
('1g7i6l9k-h3l9-0l0g-4gh7-ji2g4h3g47g2'::UUID, '5a1c0f3e-b7f3-4f4a-8ab1-dc6a8b7a81a6'::UUID, 'STOCK_LEDGER', 'STOCK_LEVEL_CHANGED', '{"skuId": "SKU-003", "locationId": "LOC-001", "previousQuantity": 155, "newQuantity": 150, "source": "INTERNAL"}', NOW() - INTERVAL '3 day', true, NOW() - INTERVAL '3 day'),
('1g7i6l9k-h3l9-0l0g-4gh7-ji2g4h3g47g3'::UUID, '7c3e2h5g-d9h5-6h6c-0cd3-fe8c0d9c03c1'::UUID, 'LOW_STOCK_ALERT', 'LOW_STOCK_DETECTED', '{"alertId": "7c3e2h5g-d9h5-6h6c-0cd3-fe8c0d9c03c1", "skuId": "SKU-006", "locationId": "LOC-001", "alertLevel": "LOW", "currentQuantity": 10, "threshold": 20}', NOW() - INTERVAL '3 hour', true, NOW() - INTERVAL '3 hour'),
('1g7i6l9k-h3l9-0l0g-4gh7-ji2g4h3g47g4'::UUID, '8d4f3i6h-e0i6-7i7d-1de4-gf9d1e0d14d1'::UUID, 'RESERVATION', 'RESERVATION_CREATED', '{"reservationId": "8d4f3i6h-e0i6-7i7d-1de4-gf9d1e0d14d1", "skuId": "SKU-001", "locationId": "LOC-001", "quantity": 10, "expiresAt": "' || TO_CHAR((NOW() + INTERVAL '24 hour'), 'YYYY-MM-DD"T"HH24:MI:SS"Z"') || '"}', NOW(), false, NULL),
('1g7i6l9k-h3l9-0l0g-4gh7-ji2g4h3g47g5'::UUID, '5a1c0f3e-b7f3-4f4a-8ab1-dc6a8b7a81a1'::UUID, 'STOCK_LEDGER', 'STOCK_VALIDATION', '{"skuId": "SKU-001", "locationId": "LOC-001", "requestedQuantity": 10, "available": true, "availableQuantity": 90}', NOW() - INTERVAL '12 hour', true, NOW() - INTERVAL '12 hour');