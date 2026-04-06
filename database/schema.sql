-- Smart Inventory AI - SQLite Schema
-- Version 1.0

PRAGMA journal_mode=WAL;
PRAGMA foreign_keys=ON;

-- ─────────────────────────────────────────────────────────────────────────────
-- Inventory table: stores all inventory entries
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS inventory (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    type        TEXT    NOT NULL CHECK(type IN ('bought','sold','waste','transfer','unknown')),
    item        TEXT    NOT NULL,
    quantity    TEXT    NOT NULL,
    price       REAL    DEFAULT 0.0,
    source      TEXT    DEFAULT 'unknown',
    timestamp   TEXT    NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE INDEX IF NOT EXISTS idx_inventory_type      ON inventory(type);
CREATE INDEX IF NOT EXISTS idx_inventory_item      ON inventory(item);
CREATE INDEX IF NOT EXISTS idx_inventory_timestamp ON inventory(timestamp);

-- ─────────────────────────────────────────────────────────────────────────────
-- Users table: privacy-preserving with encrypted real identity
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    real_name_enc     TEXT    NOT NULL,      -- AES-256 encrypted
    phone_enc         TEXT,                  -- AES-256 encrypted
    pseudonym         TEXT    NOT NULL UNIQUE,
    did               TEXT    NOT NULL UNIQUE, -- Decentralized Identifier
    reputation_score  REAL    DEFAULT 50.0,
    success_count     INTEGER DEFAULT 0,
    dispute_win_count INTEGER DEFAULT 0,
    dispute_total     INTEGER DEFAULT 0,
    response_speed    REAL    DEFAULT 0.5,   -- 0-1 normalized
    delivery_score    REAL    DEFAULT 0.5,   -- 0-1 normalized
    transaction_count INTEGER DEFAULT 0,
    created_at        TEXT    DEFAULT (datetime('now','localtime')),
    last_active       TEXT    DEFAULT (datetime('now','localtime'))
);

-- Insert a default demo user (self)
INSERT OR IGNORE INTO users
    (real_name_enc, phone_enc, pseudonym, did, reputation_score)
VALUES
    ('ENCRYPTED:DemoMerchant', 'ENCRYPTED:03001234567',
     'Merchant #001', 'did:local:merchant001', 75.0);

-- ─────────────────────────────────────────────────────────────────────────────
-- Transactions: B2B trade records
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS transactions (
    transaction_id TEXT    PRIMARY KEY,
    seller_did     TEXT    NOT NULL,
    buyer_did      TEXT    NOT NULL,
    item           TEXT    NOT NULL,
    quantity       TEXT    NOT NULL,
    price          REAL    NOT NULL,
    total_value    REAL    GENERATED ALWAYS AS (
                        CAST(REPLACE(REPLACE(quantity,'kg',''),'pcs','') AS REAL) * price
                    ) VIRTUAL,
    timestamp      TEXT    DEFAULT (datetime('now','localtime')),
    status         TEXT    DEFAULT 'pending'
                           CHECK(status IN ('pending','confirmed','completed','disputed','cancelled')),
    notes          TEXT
);

CREATE INDEX IF NOT EXISTS idx_tx_seller ON transactions(seller_did);
CREATE INDEX IF NOT EXISTS idx_tx_buyer  ON transactions(buyer_did);
CREATE INDEX IF NOT EXISTS idx_tx_status ON transactions(status);

-- ─────────────────────────────────────────────────────────────────────────────
-- Transaction hashes: tamper-evident blockchain-ready records
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS transaction_hashes (
    transaction_id TEXT PRIMARY KEY,
    hash           TEXT NOT NULL,           -- SHA-256 of transaction data
    created_at     TEXT DEFAULT (datetime('now','localtime')),
    FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- Disputes: tamper-proof dispute resolution records
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS disputes (
    dispute_id     TEXT    PRIMARY KEY,
    transaction_id TEXT    NOT NULL,
    raised_by_did  TEXT    NOT NULL,
    evidence       TEXT    NOT NULL,
    status         TEXT    DEFAULT 'open'
                           CHECK(status IN ('open','under_review','resolved','dismissed')),
    resolution     TEXT,
    admin_notes    TEXT,
    created_at     TEXT    DEFAULT (datetime('now','localtime')),
    resolved_at    TEXT,
    FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id)
);

CREATE INDEX IF NOT EXISTS idx_disputes_tx     ON disputes(transaction_id);
CREATE INDEX IF NOT EXISTS idx_disputes_status ON disputes(status);

-- ─────────────────────────────────────────────────────────────────────────────
-- Trade offers: open buy/sell offers for matching
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS trade_offers (
    offer_id       TEXT    PRIMARY KEY,
    user_did       TEXT    NOT NULL,
    type           TEXT    NOT NULL CHECK(type IN ('buy','sell')),
    item           TEXT    NOT NULL,
    quantity       TEXT    NOT NULL,
    target_price   REAL    DEFAULT 0.0,
    location_zone  INTEGER DEFAULT 1,       -- 1-10 proximity zone
    urgency        REAL    DEFAULT 0.5,     -- 0-1
    freshness      REAL    DEFAULT 1.0,     -- 0-1, decays over time
    is_active      INTEGER DEFAULT 1,
    created_at     TEXT    DEFAULT (datetime('now','localtime')),
    expires_at     TEXT
);

CREATE INDEX IF NOT EXISTS idx_offers_type ON trade_offers(type);
CREATE INDEX IF NOT EXISTS idx_offers_item ON trade_offers(item);

-- ─────────────────────────────────────────────────────────────────────────────
-- Seed mock trade participants for matching demo
-- ─────────────────────────────────────────────────────────────────────────────
INSERT OR IGNORE INTO users
    (real_name_enc, pseudonym, did, reputation_score, success_count, response_speed, delivery_score, transaction_count)
VALUES
    ('ENCRYPTED:Trader2', 'Trader #042', 'did:local:trader042', 88.5, 45, 0.85, 0.90, 50),
    ('ENCRYPTED:Trader3', 'Merchant #118', 'did:local:merchant118', 72.0, 28, 0.70, 0.75, 32),
    ('ENCRYPTED:Trader4', 'Supplier #007', 'did:local:supplier007', 95.2, 120, 0.95, 0.98, 130),
    ('ENCRYPTED:Trader5', 'Buyer #033', 'did:local:buyer033', 61.5, 15, 0.60, 0.65, 18),
    ('ENCRYPTED:Trader6', 'Wholesaler #09', 'did:local:wholesale009', 83.0, 67, 0.80, 0.85, 74);

INSERT OR IGNORE INTO trade_offers
    (offer_id, user_did, type, item, quantity, target_price, location_zone, urgency, freshness)
VALUES
    ('offer-001', 'did:local:trader042',   'sell', 'chicken',  '50kg',  620.0, 2, 0.8, 0.95),
    ('offer-002', 'did:local:merchant118', 'buy',  'chicken',  '20kg',  650.0, 3, 0.7, 0.90),
    ('offer-003', 'did:local:supplier007', 'sell', 'beef',     '100kg', 850.0, 1, 0.5, 0.85),
    ('offer-004', 'did:local:buyer033',    'buy',  'beef',     '10kg',  900.0, 4, 0.9, 0.80),
    ('offer-005', 'did:local:wholesale009','sell', 'mutton',   '200kg', 1100.0,2, 0.4, 0.70),
    ('offer-006', 'did:local:trader042',   'sell', 'rice',     '500kg', 110.0, 2, 0.3, 0.60),
    ('offer-007', 'did:local:supplier007', 'sell', 'flour',    '300kg', 85.0,  1, 0.5, 0.75),
    ('offer-008', 'did:local:merchant118', 'buy',  'vegetables','100kg',80.0,  3, 0.8, 0.95);
