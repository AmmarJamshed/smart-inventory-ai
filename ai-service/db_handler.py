"""
Database handler for the Python AI service.
Manages SQLite operations for inventory, users, transactions, and disputes.
"""

import sqlite3
import hashlib
import uuid
import json
import logging
from pathlib import Path
from datetime import datetime

logger = logging.getLogger(__name__)

DB_PATH = Path(__file__).parent.parent / "data" / "inventory.db"
SCHEMA_PATH = Path(__file__).parent.parent / "database" / "schema.sql"


def get_connection() -> sqlite3.Connection:
    DB_PATH.parent.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(str(DB_PATH))
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA journal_mode=WAL")
    conn.execute("PRAGMA foreign_keys=ON")
    return conn


def init_db():
    """Initialize database with schema.sql."""
    if not SCHEMA_PATH.exists():
        logger.warning(f"Schema file not found at {SCHEMA_PATH}")
        _create_tables_manually()
        return

    with get_connection() as conn:
        schema = SCHEMA_PATH.read_text(encoding="utf-8")
        conn.executescript(schema)
    logger.info(f"Database initialized at {DB_PATH}")


def _create_tables_manually():
    """Fallback: create tables without schema file."""
    with get_connection() as conn:
        conn.executescript("""
            CREATE TABLE IF NOT EXISTS inventory (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                type TEXT NOT NULL DEFAULT 'unknown',
                item TEXT NOT NULL,
                quantity TEXT NOT NULL,
                price REAL DEFAULT 0.0,
                source TEXT DEFAULT 'unknown',
                timestamp TEXT DEFAULT (datetime('now','localtime'))
            );
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                real_name_enc TEXT NOT NULL,
                phone_enc TEXT,
                pseudonym TEXT NOT NULL UNIQUE,
                did TEXT NOT NULL UNIQUE,
                reputation_score REAL DEFAULT 50.0,
                success_count INTEGER DEFAULT 0,
                transaction_count INTEGER DEFAULT 0,
                created_at TEXT DEFAULT (datetime('now','localtime'))
            );
            CREATE TABLE IF NOT EXISTS transactions (
                transaction_id TEXT PRIMARY KEY,
                seller_did TEXT NOT NULL,
                buyer_did TEXT NOT NULL,
                item TEXT NOT NULL,
                quantity TEXT NOT NULL,
                price REAL NOT NULL,
                timestamp TEXT DEFAULT (datetime('now','localtime')),
                status TEXT DEFAULT 'pending'
            );
            CREATE TABLE IF NOT EXISTS transaction_hashes (
                transaction_id TEXT PRIMARY KEY,
                hash TEXT NOT NULL,
                created_at TEXT DEFAULT (datetime('now','localtime'))
            );
            CREATE TABLE IF NOT EXISTS disputes (
                dispute_id TEXT PRIMARY KEY,
                transaction_id TEXT NOT NULL,
                raised_by_did TEXT NOT NULL DEFAULT 'did:local:merchant001',
                evidence TEXT NOT NULL,
                status TEXT DEFAULT 'open',
                resolution TEXT,
                created_at TEXT DEFAULT (datetime('now','localtime'))
            );
            CREATE TABLE IF NOT EXISTS trade_offers (
                offer_id TEXT PRIMARY KEY,
                user_did TEXT NOT NULL,
                type TEXT NOT NULL,
                item TEXT NOT NULL,
                quantity TEXT NOT NULL,
                target_price REAL DEFAULT 0.0,
                location_zone INTEGER DEFAULT 1,
                urgency REAL DEFAULT 0.5,
                freshness REAL DEFAULT 1.0,
                is_active INTEGER DEFAULT 1,
                created_at TEXT DEFAULT (datetime('now','localtime'))
            );
            INSERT OR IGNORE INTO users (real_name_enc, pseudonym, did, reputation_score)
            VALUES ('ENCRYPTED:DemoMerchant', 'Merchant #001', 'did:local:merchant001', 75.0);
        """)


# ─────────────────────────────────────────────────────────────────────────────
# Inventory
# ─────────────────────────────────────────────────────────────────────────────

def save_inventory(item: dict) -> dict:
    """Save inventory item and return with generated ID and timestamp."""
    ts = item.get("timestamp") or datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    with get_connection() as conn:
        cursor = conn.execute(
            "INSERT INTO inventory (type, item, quantity, price, source, timestamp) VALUES (?,?,?,?,?,?)",
            (item["type"], item["item"], item["quantity"],
             float(item.get("price", 0)), item.get("source", "unknown"), ts)
        )
        item["id"] = cursor.lastrowid
        item["timestamp"] = ts
    return item


def get_all_inventory() -> list:
    with get_connection() as conn:
        rows = conn.execute(
            "SELECT * FROM inventory ORDER BY timestamp DESC LIMIT 500"
        ).fetchall()
    return [dict(r) for r in rows]


def get_inventory_by_item(item_name: str) -> list:
    with get_connection() as conn:
        rows = conn.execute(
            "SELECT * FROM inventory WHERE LOWER(item) LIKE ? ORDER BY timestamp DESC",
            (f"%{item_name.lower()}%",)
        ).fetchall()
    return [dict(r) for r in rows]


# ─────────────────────────────────────────────────────────────────────────────
# Transactions
# ─────────────────────────────────────────────────────────────────────────────

def save_transaction(tx: dict) -> dict:
    """Save transaction and its SHA-256 hash."""
    tx_id = tx.get("transaction_id") or str(uuid.uuid4())
    tx["transaction_id"] = tx_id
    ts = tx.get("timestamp") or datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    with get_connection() as conn:
        conn.execute(
            """INSERT OR REPLACE INTO transactions
               (transaction_id, seller_did, buyer_did, item, quantity, price, timestamp, status)
               VALUES (?,?,?,?,?,?,?,?)""",
            (tx_id, tx["seller_did"], tx["buyer_did"],
             tx["item"], tx["quantity"], float(tx["price"]),
             ts, tx.get("status", "pending"))
        )
        # Generate and store SHA-256 hash
        hash_input = f"{tx_id}:{tx['seller_did']}:{tx['buyer_did']}:{tx['item']}:{tx['quantity']}:{tx['price']}:{ts}"
        tx_hash = hashlib.sha256(hash_input.encode()).hexdigest()
        conn.execute(
            "INSERT OR REPLACE INTO transaction_hashes (transaction_id, hash) VALUES (?,?)",
            (tx_id, tx_hash)
        )
        tx["hash"] = tx_hash
        tx["timestamp"] = ts
    return tx


def get_transaction(tx_id: str) -> dict | None:
    with get_connection() as conn:
        row = conn.execute(
            """SELECT t.*, h.hash FROM transactions t
               LEFT JOIN transaction_hashes h ON t.transaction_id = h.transaction_id
               WHERE t.transaction_id = ?""",
            (tx_id,)
        ).fetchone()
    return dict(row) if row else None


# ─────────────────────────────────────────────────────────────────────────────
# Disputes
# ─────────────────────────────────────────────────────────────────────────────

def create_dispute(tx_id: str, evidence: str, raised_by: str = "did:local:merchant001") -> dict:
    """Create a dispute for a transaction."""
    dispute_id = f"DIS-{uuid.uuid4().hex[:8].upper()}"
    ts = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    with get_connection() as conn:
        conn.execute(
            """INSERT INTO disputes (dispute_id, transaction_id, raised_by_did, evidence, status, created_at)
               VALUES (?,?,?,?,?,?)""",
            (dispute_id, tx_id, raised_by, evidence, "open", ts)
        )
    return {
        "dispute_id": dispute_id,
        "transaction_id": tx_id,
        "evidence": evidence,
        "status": "open",
        "resolution": None,
        "created_at": ts,
    }


def get_all_disputes() -> list:
    with get_connection() as conn:
        rows = conn.execute(
            "SELECT * FROM disputes ORDER BY created_at DESC"
        ).fetchall()
    return [dict(r) for r in rows]


# ─────────────────────────────────────────────────────────────────────────────
# Trade Offers
# ─────────────────────────────────────────────────────────────────────────────

def get_active_offers(item: str = None, offer_type: str = None) -> list:
    query = "SELECT o.*, u.pseudonym, u.reputation_score FROM trade_offers o JOIN users u ON o.user_did = u.did WHERE o.is_active = 1"
    params = []
    if item:
        query += " AND LOWER(o.item) LIKE ?"
        params.append(f"%{item.lower()}%")
    if offer_type:
        query += " AND o.type = ?"
        params.append(offer_type.lower())
    query += " ORDER BY o.freshness DESC LIMIT 20"

    with get_connection() as conn:
        rows = conn.execute(query, params).fetchall()
    return [dict(r) for r in rows]


# ─────────────────────────────────────────────────────────────────────────────
# Analytics helpers
# ─────────────────────────────────────────────────────────────────────────────

def get_analytics_summary() -> dict:
    with get_connection() as conn:
        total = conn.execute("SELECT COUNT(*) FROM inventory").fetchone()[0]
        waste = conn.execute("SELECT COUNT(*) FROM inventory WHERE type='waste'").fetchone()[0]
        today = datetime.now().strftime("%Y-%m-%d")
        today_count = conn.execute(
            "SELECT COUNT(*) FROM inventory WHERE timestamp LIKE ?", (f"{today}%",)
        ).fetchone()[0]
        total_value = conn.execute(
            "SELECT SUM(price) FROM inventory WHERE type='bought'"
        ).fetchone()[0] or 0.0
    return {
        "total_items": total,
        "waste_count": waste,
        "waste_percentage": round(waste / total * 100, 1) if total > 0 else 0,
        "today_count": today_count,
        "total_value": round(total_value, 2),
    }
