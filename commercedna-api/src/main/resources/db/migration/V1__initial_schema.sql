-- CommerceDNA Initial Database Schema
-- Version 1.0.0
-- Razorpay Buildathon 2026 Submission

-- Merchant Identity Table
CREATE TABLE IF NOT EXISTS merchants (
    id UUID PRIMARY KEY,
    merchant_code VARCHAR(50) UNIQUE NOT NULL,
    business_name VARCHAR(255) NOT NULL,
    contact_email VARCHAR(255) NOT NULL,
    public_key_ed25519 TEXT NOT NULL,
    encrypted_razorpay_key_id TEXT NOT NULL,
    encrypted_razorpay_key_secret TEXT NOT NULL,
    encrypted_webhook_secret TEXT NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Product Catalog Table
CREATE TABLE IF NOT EXISTS products (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE CASCADE,
    sku VARCHAR(100) NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    currency VARCHAR(3) DEFAULT 'INR',
    base_price_paise BIGINT NOT NULL,
    cost_price_paise BIGINT NOT NULL,
    min_margin_percentage DECIMAL(5,4) NOT NULL,
    max_discount_percentage DECIMAL(5,4) NOT NULL,
    min_quantity INTEGER DEFAULT 1,
    max_quantity INTEGER DEFAULT 50,
    stock_quantity INTEGER DEFAULT 0,
    tags TEXT,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(merchant_id, sku)
);

-- Negotiation Proposals Table
CREATE TABLE IF NOT EXISTS proposals (
    id UUID PRIMARY KEY,
    proposal_code VARCHAR(50) UNIQUE NOT NULL,
    merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE CASCADE,
    buyer_agent_did VARCHAR(255) NOT NULL,
    sku VARCHAR(100) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price_paise BIGINT NOT NULL,
    total_amount_paise BIGINT NOT NULL,
    currency VARCHAR(3) DEFAULT 'INR',
    status VARCHAR(50) NOT NULL,
    merchant_signature TEXT,
    buyer_message TEXT,
    agent_reply TEXT,
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Orders Table
CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY,
    order_code VARCHAR(50) UNIQUE NOT NULL,
    proposal_id UUID REFERENCES proposals(id) ON DELETE SET NULL,
    merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE CASCADE,
    buyer_agent_did VARCHAR(255) NOT NULL,
    proposal_code VARCHAR(50),
    idempotency_key VARCHAR(255) UNIQUE NOT NULL,
    sku VARCHAR(100) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price_paise BIGINT NOT NULL,
    total_amount_paise BIGINT NOT NULL,
    currency VARCHAR(3) DEFAULT 'INR',
    status VARCHAR(50) NOT NULL,
    razorpay_order_id VARCHAR(255),
    razorpay_payment_id VARCHAR(255),
    razorpay_payment_link_id VARCHAR(255),
    payment_link_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Cryptographic Audit Ledger Table
CREATE TABLE IF NOT EXISTS audit_ledger (
    id UUID PRIMARY KEY,
    sequence_number BIGINT UNIQUE NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(255) NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    previous_block_hash VARCHAR(64) NOT NULL,
    block_hash VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Transactional Outbox Table
CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    retry_count INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE
);

-- Create Indexes for Performance
CREATE INDEX IF NOT EXISTS idx_merchants_code ON merchants(merchant_code);
CREATE INDEX IF NOT EXISTS idx_merchants_active ON merchants(active);

CREATE INDEX IF NOT EXISTS idx_products_merchant_id ON products(merchant_id);
CREATE INDEX IF NOT EXISTS idx_products_sku ON products(sku);
CREATE INDEX IF NOT EXISTS idx_products_active ON products(active);
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category);

CREATE INDEX IF NOT EXISTS idx_proposals_merchant_id ON proposals(merchant_id);
CREATE INDEX IF NOT EXISTS idx_proposals_buyer_agent ON proposals(buyer_agent_did);
CREATE INDEX IF NOT EXISTS idx_proposals_status ON proposals(status);
CREATE INDEX IF NOT EXISTS idx_proposals_expires_at ON proposals(expires_at);

CREATE INDEX IF NOT EXISTS idx_orders_merchant_id ON orders(merchant_id);
CREATE INDEX IF NOT EXISTS idx_orders_buyer_agent ON orders(buyer_agent_did);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_razorpay_order_id ON orders(razorpay_order_id);
CREATE INDEX IF NOT EXISTS idx_orders_idempotency_key ON orders(idempotency_key);

CREATE INDEX IF NOT EXISTS idx_audit_ledger_sequence ON audit_ledger(sequence_number);
CREATE INDEX IF NOT EXISTS idx_audit_ledger_event_type ON audit_ledger(event_type);
CREATE INDEX IF NOT EXISTS idx_audit_ledger_entity ON audit_ledger(entity_type, entity_id);

CREATE INDEX IF NOT EXISTS idx_outbox_events_status ON outbox_events(status);
CREATE INDEX IF NOT EXISTS idx_outbox_events_created_at ON outbox_events(created_at);

-- Add audit timestamp triggers
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_merchants_updated_at BEFORE UPDATE ON merchants
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_products_updated_at BEFORE UPDATE ON products
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_proposals_updated_at BEFORE UPDATE ON proposals
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_orders_updated_at BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();