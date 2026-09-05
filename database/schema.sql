-- =============================================================================
-- CommerceDNA Production Schema Definition (PostgreSQL 16 + pgvector)
-- =============================================================================

-- Enable required cryptographic and vector extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "vector";

-- -----------------------------------------------------------------------------
-- 1. Merchants Table (Cryptographic DNA & Encrypted Vault)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS merchants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_code VARCHAR(32) NOT NULL UNIQUE,
    business_name VARCHAR(128) NOT NULL,
    contact_email VARCHAR(128) NOT NULL,
    public_key_ed25519 VARCHAR(128) NOT NULL,
    encrypted_private_key_ed25519 TEXT,
    encrypted_razorpay_key_id TEXT NOT NULL,
    encrypted_razorpay_key_secret TEXT NOT NULL,
    encrypted_webhook_secret TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_merchants_code ON merchants(merchant_code);
CREATE INDEX IF NOT EXISTS idx_merchants_active ON merchants(active);

-- -----------------------------------------------------------------------------
-- 2. Catalog Products Table (Deterministic Guardrails & Vector Embedding)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS catalog_products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE CASCADE,
    sku VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(64) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    base_price_paise BIGINT NOT NULL CHECK (base_price_paise > 0),
    cost_price_paise BIGINT NOT NULL CHECK (cost_price_paise > 0),
    min_margin_percentage DOUBLE PRECISION NOT NULL CHECK (min_margin_percentage >= 0.0),
    max_discount_percentage DOUBLE PRECISION NOT NULL CHECK (max_discount_percentage >= 0.0 AND max_discount_percentage <= 1.0),
    min_quantity INT NOT NULL DEFAULT 1 CHECK (min_quantity >= 1),
    max_quantity INT NOT NULL DEFAULT 100 CHECK (max_quantity >= min_quantity),
    stock_quantity INT NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    embedding vector(768),
    tags VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_merchant_sku UNIQUE (merchant_id, sku),
    CONSTRAINT chk_base_cost_price CHECK (base_price_paise >= cost_price_paise)
);

CREATE INDEX IF NOT EXISTS idx_catalog_merchant_sku ON catalog_products(merchant_id, sku);
CREATE INDEX IF NOT EXISTS idx_catalog_category ON catalog_products(category);
CREATE INDEX IF NOT EXISTS idx_catalog_active ON catalog_products(active);

-- -----------------------------------------------------------------------------
-- 3. Intent Proposals Table (Dual-Model Compiler & Airgap Evaluation)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS intent_proposals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    proposal_code VARCHAR(64) NOT NULL UNIQUE,
    merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE CASCADE,
    buyer_agent_did VARCHAR(128) NOT NULL,
    sku VARCHAR(64) NOT NULL,
    quantity INT NOT NULL CHECK (quantity >= 1),
    proposed_unit_price_paise BIGINT NOT NULL CHECK (proposed_unit_price_paise >= 0),
    counter_unit_price_paise BIGINT,
    total_amount_paise BIGINT NOT NULL CHECK (total_amount_paise >= 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    rationale TEXT,
    buyer_public_key VARCHAR(128),
    buyer_signature VARCHAR(256),
    merchant_signature VARCHAR(256),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_proposals_code ON intent_proposals(proposal_code);
CREATE INDEX IF NOT EXISTS idx_proposals_merchant_status ON intent_proposals(merchant_id, status);
CREATE INDEX IF NOT EXISTS idx_proposals_buyer ON intent_proposals(buyer_agent_did);

-- -----------------------------------------------------------------------------
-- 4. Orders Table (Atomic Settlement & Razorpay Test Mode State Machine)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_code VARCHAR(64) NOT NULL UNIQUE,
    merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE RESTRICT,
    buyer_agent_did VARCHAR(128) NOT NULL,
    proposal_code VARCHAR(64) REFERENCES intent_proposals(proposal_code),
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    sku VARCHAR(64) NOT NULL,
    quantity INT NOT NULL CHECK (quantity >= 1),
    unit_price_paise BIGINT NOT NULL CHECK (unit_price_paise >= 0),
    total_amount_paise BIGINT NOT NULL CHECK (total_amount_paise >= 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
    razorpay_order_id VARCHAR(64),
    razorpay_payment_id VARCHAR(64),
    razorpay_payment_link_id VARCHAR(64),
    payment_link_url VARCHAR(512),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_orders_code ON orders(order_code);
CREATE INDEX IF NOT EXISTS idx_orders_idempotency ON orders(idempotency_key);
CREATE INDEX IF NOT EXISTS idx_orders_merchant ON orders(merchant_id);
CREATE INDEX IF NOT EXISTS idx_orders_rzp_order ON orders(razorpay_order_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);

-- -----------------------------------------------------------------------------
-- 5. Transactional Outbox Table (Reliable Background Dispatch)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_outbox_pending ON outbox_events(status, created_at);

-- -----------------------------------------------------------------------------
-- 6. Cryptographic Audit Ledger Table (Append-Only Merkle Hash Chain)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_ledger (
    sequence_number BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    payload_json TEXT NOT NULL,
    previous_hash VARCHAR(64) NOT NULL,
    record_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_seq_hash ON audit_ledger(sequence_number, record_hash);
CREATE INDEX IF NOT EXISTS idx_audit_entity ON audit_ledger(entity_type, entity_id);

-- -----------------------------------------------------------------------------
-- 7. Immutability Trigger: Enforce Append-Only on Audit Ledger (Zero UPDATE/DELETE)
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION trg_prevent_audit_tampering()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'CRITICAL SECURITY VIOLATION: Audit Ledger is strictly append-only. UPDATE and DELETE operations are forbidden.';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_audit_no_update_delete ON audit_ledger;
CREATE TRIGGER trg_audit_no_update_delete
BEFORE UPDATE OR DELETE ON audit_ledger
FOR EACH ROW
EXECUTE FUNCTION trg_prevent_audit_tampering();
