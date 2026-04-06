"""
Local LLM Engine — zero cloud, zero Ollama required.
Uses llama-cpp-python with a bundled TinyLlama GGUF model.

Model: TinyLlama-1.1B-Chat-v1.0 (Q4_K_M) — ~669 MB
License: Apache 2.0
Source: https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF

Priority order:
  1. Local bundled GGUF model (models/tinyllama.gguf)
  2. Ollama server (http://localhost:11434)
  3. Rule-based parser (always works offline)
"""

import json
import logging
import os
import re
from pathlib import Path
from typing import Optional

logger = logging.getLogger(__name__)

# Possible model locations (bundled or user-placed)
MODEL_SEARCH_PATHS = [
    Path(__file__).parent / "models" / "tinyllama.gguf",
    Path(__file__).parent / "models" / "tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
    Path(__file__).parent.parent / "models" / "tinyllama.gguf",
    Path(os.environ.get("SMART_INVENTORY_MODEL", "")) if os.environ.get("SMART_INVENTORY_MODEL") else Path("."),
]

_llm = None
_llm_loaded = False


def _find_model() -> Optional[Path]:
    for p in MODEL_SEARCH_PATHS:
        if p.exists() and p.suffix == ".gguf":
            return p
    return None


def _load_llm():
    global _llm, _llm_loaded
    if _llm_loaded:
        return _llm

    model_path = _find_model()
    if not model_path:
        logger.info("No local GGUF model found. Ollama/rule-based fallback will be used.")
        _llm_loaded = True
        return None

    try:
        from llama_cpp import Llama
        logger.info(f"Loading local model: {model_path}")
        _llm = Llama(
            model_path=str(model_path),
            n_ctx=2048,
            n_threads=4,
            n_gpu_layers=0,   # CPU-only for maximum compatibility
            verbose=False,
        )
        logger.info("Local model loaded successfully.")
    except Exception as e:
        logger.warning(f"Failed to load local model: {e}")
        _llm = None

    _llm_loaded = True
    return _llm


def parse_with_local_model(text: str) -> Optional[dict]:
    """Parse inventory text using the local GGUF model."""
    llm = _load_llm()
    if not llm:
        return None

    prompt = (
        "<|system|>\n"
        "You are an inventory parser. Extract structured data from inventory messages.\n"
        "Return ONLY valid JSON with keys: type, item, quantity, price, source.\n"
        "type must be one of: bought, sold, waste, transfer\n"
        "</s>\n"
        "<|user|>\n"
        f'Parse: "{text}"\n'
        "Examples:\n"
        '  "Bought 10kg chicken @ 650" -> {"type":"bought","item":"chicken","quantity":"10kg","price":650,"source":"market"}\n'
        '  "2kg gosht zaya hogaya"     -> {"type":"waste","item":"beef","quantity":"2kg","price":0,"source":"unknown"}\n'
        'Return only JSON:\n'
        "</s>\n"
        "<|assistant|>\n"
    )

    try:
        output = llm(
            prompt,
            max_tokens=120,
            temperature=0.1,
            stop=["</s>", "\n\n", "<|user|>"],
        )
        raw = output["choices"][0]["text"].strip()
        logger.debug(f"Local model raw output: {raw}")

        json_match = re.search(r'\{[^}]+\}', raw, re.DOTALL)
        if json_match:
            parsed = json.loads(json_match.group())
            parsed["price"] = float(parsed.get("price", 0) or 0)
            return parsed
    except Exception as e:
        logger.warning(f"Local model inference error: {e}")

    return None


def download_model(model_dir: Optional[Path] = None) -> Path:
    """
    Download TinyLlama GGUF model from HuggingFace.
    ~669 MB download. Runs once, cached locally.
    """
    if model_dir is None:
        model_dir = Path(__file__).parent / "models"
    model_dir.mkdir(parents=True, exist_ok=True)
    dest = model_dir / "tinyllama.gguf"

    if dest.exists():
        logger.info(f"Model already downloaded: {dest}")
        return dest

    try:
        from huggingface_hub import hf_hub_download
        logger.info("Downloading TinyLlama-1.1B model (~669 MB)...")
        path = hf_hub_download(
            repo_id="TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF",
            filename="tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
            local_dir=str(model_dir),
            local_dir_use_symlinks=False,
        )
        # Rename to standard name
        import shutil
        shutil.copy(path, dest)
        logger.info(f"Model downloaded to {dest}")
        return dest
    except Exception as e:
        logger.error(f"Model download failed: {e}")
        raise


def get_model_status() -> dict:
    """Return current model status for health check."""
    model_path = _find_model()
    llm = _llm  # don't trigger load, just check state

    return {
        "local_model_found": model_path is not None,
        "local_model_path": str(model_path) if model_path else None,
        "local_model_loaded": llm is not None,
        "model_size_mb": round(model_path.stat().st_size / 1_048_576, 1) if model_path else 0,
    }
