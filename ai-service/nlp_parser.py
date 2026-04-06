"""
NLP Parser — English + Urdu inventory message parser.
Strategy: Try local Ollama LLM first, fall back to rule-based parser.
"""

import re
import json
import logging
from typing import Optional

logger = logging.getLogger(__name__)

# ─────────────────────────────────────────────────────────────────────────────
# Vocabulary maps for Urdu/Roman-Urdu keywords
# ─────────────────────────────────────────────────────────────────────────────
TYPE_KEYWORDS = {
    "bought":   ["bought", "buy", "purchase", "kharida", "khareed", "liya", "mila",
                 "le liya", "mangwaya", "order", "aya", "aaya", "mil gaya"],
    "sold":     ["sold", "sell", "sale", "becha", "bech diya", "farokht", "nikala",
                 "de diya", "gaya", "customer ko"],
    "waste":    ["waste", "wasted", "zaya", "barbaad", "kharab", "hogaya", "ho gaya",
                 "expiry", "expired", "feka", "phenk", "nahi chala", "spoiled", "rot"],
    "transfer": ["transfer", "bheja", "shift", "move", "bhej diya", "branch",
                 "doosri jagah", "relocated"],
}

UNITS = ["kg", "g", "gram", "grams", "liter", "liters", "litre", "litres",
         "l", "ml", "piece", "pieces", "pcs", "pc", "dozen", "dzn",
         "box", "boxes", "bag", "bags", "pack", "packs", "sack", "sacks",
         "kilo", "kilos", "quintal"]

SOURCE_KEYWORDS = {
    "market":    ["market", "bazaar", "baazar", "bazar", "mandi"],
    "supplier":  ["supplier", "vendor", "sarabaan", "thekedar", "company"],
    "farm":      ["farm", "khet", "garden", "direct"],
    "store":     ["store", "shop", "dukaan", "godown", "warehouse"],
    "customer":  ["customer", "grahak", "client", "buyer"],
    "branch":    ["branch", "outlet", "counter"],
}

ITEM_VOCAB = [
    "chicken", "beef", "mutton", "fish", "gosht", "murgh", "machli",
    "rice", "chawal", "flour", "aata", "sugar", "cheeni", "salt", "namak",
    "oil", "tel", "butter", "ghee", "milk", "doodh", "eggs", "anday",
    "tomato", "tamatar", "onion", "pyaz", "potato", "aloo", "vegetables",
    "sabzi", "fruit", "phal", "bread", "roti", "naan", "masala", "spices",
    "lentils", "dal", "beans", "lobia", "peas", "matar", "carrot", "gajar",
    "spinach", "palak", "coriander", "dhaniya", "ginger", "adrak",
    "garlic", "lehsan", "chilli", "mirch", "black pepper", "kali mirch",
]


def parse_inventory_text(text: str) -> dict:
    """
    Main entry point. Priority:
      1. Local bundled GGUF model (TinyLlama) — offline, no install needed
      2. Ollama server (optional enhancement)
      3. Rule-based parser (always works)
    """
    # 1. Try local GGUF model
    try:
        from local_model import parse_with_local_model
        result = parse_with_local_model(text)
        if result and _is_valid_result(result):
            result["needs_confirmation"] = False
            logger.info(f"Local model parsed: {result}")
            return result
    except Exception as e:
        logger.debug(f"Local model skip: {e}")

    # 2. Try Ollama (if running)
    try:
        result = _try_ollama_parse(text)
        if result and _is_valid_result(result):
            result["needs_confirmation"] = False
            logger.info(f"Ollama parsed: {result}")
            return result
    except Exception as e:
        logger.debug(f"Ollama unavailable: {e}")

    # 3. Rule-based fallback
    result = _rule_based_parse(text)
    result["needs_confirmation"] = True
    logger.info(f"Rule-based parsed: {result}")
    return result


def _try_ollama_parse(text: str) -> Optional[dict]:
    """Try parsing via local Ollama LLM."""
    import requests

    prompt = (
        'You are an inventory parser for a restaurant/retail business. '
        'Parse the following message into a JSON object.\n\n'
        f'Message: "{text}"\n\n'
        'Return ONLY a JSON object with these exact fields:\n'
        '- type: one of "bought", "sold", "waste", "transfer"\n'
        '- item: the product name in English (e.g. "chicken")\n'
        '- quantity: amount with unit (e.g. "10kg", "5 dozen")\n'
        '- price: numeric price per unit, 0 if not mentioned\n'
        '- source: where it came from/went to (e.g. "market", "supplier")\n\n'
        'The message may be in English, Urdu, or Roman Urdu.\n'
        'Examples:\n'
        '  "Bought 10kg chicken @ 650/kg" → {"type":"bought","item":"chicken","quantity":"10kg","price":650,"source":"market"}\n'
        '  "Aaj 2kg gosht zaya hogaya" → {"type":"waste","item":"beef","quantity":"2kg","price":0,"source":"unknown"}\n\n'
        'Return ONLY the JSON, no explanation.'
    )

    response = requests.post(
        "http://localhost:11434/api/generate",
        json={"model": "mistral", "prompt": prompt, "stream": False, "format": "json"},
        timeout=25,
    )
    response.raise_for_status()
    raw = response.json().get("response", "").strip()

    # Extract JSON from response
    json_match = re.search(r'\{.*\}', raw, re.DOTALL)
    if json_match:
        parsed = json.loads(json_match.group())
        parsed["price"] = float(parsed.get("price", 0) or 0)
        return parsed

    return None


def _rule_based_parse(text: str) -> dict:
    """Deterministic rule-based parser for English + Urdu/Roman-Urdu."""
    text_lower = text.lower().strip()
    words = text_lower.split()

    # ── Determine type ────────────────────────────────────────────────────────
    # Use word-boundary matching for short keywords to avoid false substring hits
    item_type = "unknown"
    for t, keywords in TYPE_KEYWORDS.items():
        matched = False
        for kw in keywords:
            # For single words, check word boundary; for phrases, use substring
            if " " in kw:
                if kw in text_lower:
                    matched = True; break
            else:
                # Pad text with spaces for whole-word matching
                if f" {kw} " in f" {text_lower} ":
                    matched = True; break
        if matched:
            item_type = t
            break

    # ── Extract quantity ──────────────────────────────────────────────────────
    unit_pattern = "|".join(re.escape(u) for u in sorted(UNITS, key=len, reverse=True))
    qty_match = re.search(rf'(\d+(?:\.\d+)?)\s*({unit_pattern})', text_lower, re.IGNORECASE)
    quantity = qty_match.group(0).strip() if qty_match else _extract_number(text_lower) or "1"

    # ── Extract price ─────────────────────────────────────────────────────────
    price = 0.0
    # Patterns: @650, @ 650, 650/kg, rs.650, ₨650, rupay 650, 650 rupees
    price_patterns = [
        r'@\s*(\d+(?:\.\d+)?)',
        r'(?:rs\.?|rupay|rupees|₨|pkr)\s*(\d+(?:\.\d+)?)',
        r'(\d+(?:\.\d+)?)\s*(?:per|/)\s*(?:' + unit_pattern + r')',
        r'(\d+(?:\.\d+)?)\s*(?:rupay|rupees|rs)',
    ]
    for pattern in price_patterns:
        m = re.search(pattern, text_lower, re.IGNORECASE)
        if m:
            price = float(m.group(1))
            break

    # ── Extract item name ──────────────────────────────────────────────────────
    item = _extract_item(text_lower, words, qty_match)

    # ── Extract source ─────────────────────────────────────────────────────────
    source = "unknown"
    for src, keywords in SOURCE_KEYWORDS.items():
        if any(kw in text_lower for kw in keywords):
            source = src
            break

    return {
        "type":     item_type,
        "item":     item,
        "quantity": quantity,
        "price":    price,
        "source":   source,
    }


def _extract_item(text_lower: str, words: list, qty_match) -> str:
    """Try to extract item name from text."""
    # Check known vocabulary
    for item in sorted(ITEM_VOCAB, key=len, reverse=True):
        if item in text_lower:
            return item

    # Fallback: word before the quantity, after the type keyword
    if qty_match:
        pos = text_lower.find(qty_match.group(0))
        before = text_lower[:pos].strip().split()
        candidates = [
            w for w in reversed(before)
            if len(w) > 2 and w.isalpha()
            and w not in {"the", "aur", "and", "kal", "aaj", "tha", "hai", "ka", "ko"}
        ]
        if candidates:
            return candidates[0]

    # Last resort: second word
    alpha_words = [w for w in words if w.isalpha() and len(w) > 2]
    return alpha_words[1] if len(alpha_words) > 1 else "item"


def _extract_number(text: str) -> Optional[str]:
    """Extract a standalone number as quantity."""
    m = re.search(r'\b(\d+(?:\.\d+)?)\b', text)
    return m.group(1) if m else None


def _is_valid_result(result: dict) -> bool:
    """Check if parsed result has required fields."""
    required = ["type", "item", "quantity", "price", "source"]
    return (
        all(k in result for k in required)
        and result.get("type") in {"bought", "sold", "waste", "transfer", "unknown"}
        and isinstance(result.get("price"), (int, float))
    )
