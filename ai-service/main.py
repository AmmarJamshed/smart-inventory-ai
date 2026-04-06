"""
Smart Inventory AI — Python FastAPI Service
Port: 8000
Handles: NLP parsing, inventory CRUD, transactions, disputes, analytics
"""

import logging
import uuid
from datetime import datetime
from typing import Optional

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, field_validator

from nlp_parser import parse_inventory_text
from db_handler import (
    init_db, save_inventory, get_all_inventory,
    save_transaction, get_transaction,
    create_dispute, get_all_disputes,
    get_active_offers, get_analytics_summary,
)

# ─────────────────────────────────────────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s — %(message)s",
)
logger = logging.getLogger(__name__)

# ─────────────────────────────────────────────────────────────────────────────
app = FastAPI(
    title="Smart Inventory AI",
    description="Offline-first AI inventory system with NLP, trade matching, and dispute resolution",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.on_event("startup")
async def startup():
    init_db()
    logger.info("Database initialized. Smart Inventory AI service ready.")


# ─────────────────────────────────────────────────────────────────────────────
# Pydantic Models
# ─────────────────────────────────────────────────────────────────────────────

class ParseRequest(BaseModel):
    text: str

    @field_validator("text")
    @classmethod
    def text_not_empty(cls, v):
        if not v or not v.strip():
            raise ValueError("text cannot be empty")
        return v.strip()


class ParseResponse(BaseModel):
    type: str
    item: str
    quantity: str
    price: float
    source: str
    needs_confirmation: bool = False


class InventoryItemIn(BaseModel):
    type: str
    item: str
    quantity: str
    price: float = 0.0
    source: str = "unknown"
    timestamp: Optional[str] = None


class TransactionIn(BaseModel):
    seller_did: str
    buyer_did: str
    item: str
    quantity: str
    price: float
    status: str = "pending"
    notes: Optional[str] = None


class DisputeIn(BaseModel):
    transaction_id: str
    evidence: str
    raised_by_did: str = "did:local:merchant001"


class TradeSearchRequest(BaseModel):
    item: str
    type: str = "sell"          # what counterparty we need
    quantity: Optional[str] = None
    target_price: Optional[float] = None


# ─────────────────────────────────────────────────────────────────────────────
# Endpoints
# ─────────────────────────────────────────────────────────────────────────────

@app.get("/health")
def health():
    return {"status": "ok", "service": "Smart Inventory AI", "version": "1.0.0"}


# ── NLP Parsing ───────────────────────────────────────────────────────────────

@app.post("/parse", response_model=ParseResponse)
async def parse_text(request: ParseRequest):
    """Parse natural language inventory message into structured data."""
    logger.info(f"Parsing: {request.text[:80]}")
    try:
        result = parse_inventory_text(request.text)
        return ParseResponse(**result)
    except Exception as e:
        logger.error(f"Parse error: {e}")
        raise HTTPException(status_code=500, detail=f"Parse failed: {str(e)}")


# ── Inventory ─────────────────────────────────────────────────────────────────

@app.post("/inventory")
async def add_inventory(item: InventoryItemIn):
    """Save an inventory item to the database."""
    try:
        saved = save_inventory(item.model_dump())
        return {"success": True, "item": saved}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/inventory")
async def list_inventory():
    """Get all inventory items."""
    try:
        items = get_all_inventory()
        return {"items": items, "count": len(items)}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/analytics")
async def analytics():
    """Get inventory analytics summary."""
    try:
        return get_analytics_summary()
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# ── Transactions ──────────────────────────────────────────────────────────────

@app.post("/transaction")
async def create_transaction(tx: TransactionIn):
    """Create a new transaction with tamper-evident hash."""
    try:
        tx_data = tx.model_dump()
        tx_data["transaction_id"] = f"TX-{uuid.uuid4().hex[:12].upper()}"
        saved = save_transaction(tx_data)
        return {"success": True, "transaction": saved}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/transaction/{tx_id}")
async def get_tx(tx_id: str):
    """Look up a transaction by ID."""
    tx = get_transaction(tx_id)
    if not tx:
        raise HTTPException(status_code=404, detail=f"Transaction {tx_id} not found")
    return tx


# ── Disputes ──────────────────────────────────────────────────────────────────

@app.post("/dispute")
async def open_dispute(dispute: DisputeIn):
    """Open a dispute for a transaction."""
    tx = get_transaction(dispute.transaction_id)
    if not tx:
        raise HTTPException(
            status_code=404,
            detail=f"Transaction {dispute.transaction_id} not found"
        )
    try:
        result = create_dispute(
            dispute.transaction_id,
            dispute.evidence,
            dispute.raised_by_did,
        )
        return {"success": True, "dispute": result}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/disputes")
async def list_disputes():
    """Get all disputes."""
    try:
        return {"disputes": get_all_disputes()}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# ── Trade Matching ────────────────────────────────────────────────────────────

@app.post("/trade/search")
async def search_trade(req: TradeSearchRequest):
    """Find matching trade offers for given item and type."""
    try:
        counterpart_type = "buy" if req.type.lower() == "sell" else "sell"
        offers = get_active_offers(item=req.item, offer_type=counterpart_type)

        # Basic scoring (detailed scoring done in C++ engine)
        scored = []
        for offer in offers:
            score = _basic_match_score(offer, req)
            scored.append({**offer, "match_score": score})

        scored.sort(key=lambda x: x["match_score"], reverse=True)
        return {"matches": scored[:5], "total_found": len(scored)}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


def _basic_match_score(offer: dict, req: TradeSearchRequest) -> float:
    """Simple match scoring — detailed version in C++ engine."""
    score = 0.5  # base
    score += offer.get("reputation_score", 50) / 100 * 0.3
    score += offer.get("freshness", 0.5) * 0.2

    if req.target_price and req.target_price > 0:
        offer_price = offer.get("target_price", 0)
        if offer_price > 0:
            price_diff_pct = abs(offer_price - req.target_price) / req.target_price
            score += max(0, 0.25 - price_diff_pct * 0.25)

    return min(1.0, round(score, 3))


# ─────────────────────────────────────────────────────────────────────────────
if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True, log_level="info")
