╔══════════════════════════════════════════════════════════════════╗
║           SMART INVENTORY AI v1.0.0                             ║
║   Offline-First AI Inventory + B2B Trading System               ║
╚══════════════════════════════════════════════════════════════════╝

  GitHub:  https://github.com/AmmarJamshed/smart-inventory-ai
  License: MIT (Free to use, modify, distribute)

══════════════════════════════════════════════════════════════════
  QUICK START (3 steps)
══════════════════════════════════════════════════════════════════

  STEP 1 — Install Java 17+
    Download: https://adoptium.net  (Free, click "Latest LTS")
    Install, then continue.

  STEP 2 — Install Python 3.10+
    Download: https://python.org/downloads
    ⚠ IMPORTANT: Tick "Add Python to PATH" during install!

  STEP 3 — Run the app
    Double-click: SmartInventoryAI.bat

  That's it! The app sets up everything else automatically.

══════════════════════════════════════════════════════════════════
  OPTIONAL: Download AI Model for smarter NLP
══════════════════════════════════════════════════════════════════

  Double-click: download_ai_model.bat
  Downloads TinyLlama 1.1B (~669 MB) — runs 100% offline.
  Without it, the built-in rule-based parser handles English + Urdu.

══════════════════════════════════════════════════════════════════
  WHAT IT DOES
══════════════════════════════════════════════════════════════════

  • Type inventory in natural language (English or Urdu):
      "Bought 10kg chicken @ 650/kg from market"
      "Aaj 2kg gosht zaya hogaya"
      "Sold 5 dozen eggs @ 180 rupay"

  • AI parses it → structured table entry (confirm before saving)

  • Analytics: pie chart, bar chart, value trend

  • B2B Trade: find buyers/sellers anonymously (pseudonyms only)

  • Dispute Resolution: SHA-256 tamper-proof records

  • Dark Mode: toggle from sidebar

  • Everything runs OFFLINE. No internet needed after setup.

══════════════════════════════════════════════════════════════════
  FILES IN THIS FOLDER
══════════════════════════════════════════════════════════════════

  SmartInventoryAI.bat     ← Run this to start the app
  download_ai_model.bat    ← Optional: download TinyLlama model
  smart-inventory-ai.jar   ← Main application (JavaFX)
  ai-service\              ← AI/NLP backend (Python)
  models\                  ← AI model goes here (after download)
  data\                    ← Your database (auto-created)
  schema.sql               ← Database structure

══════════════════════════════════════════════════════════════════
  SYSTEM REQUIREMENTS
══════════════════════════════════════════════════════════════════

  OS:      Windows 10 or 11 (64-bit)
  Java:    17 or newer  →  https://adoptium.net
  Python:  3.10 or newer → https://python.org
  RAM:     4 GB minimum (8 GB recommended with AI model)
  Storage: 200 MB base  +  700 MB for AI model (optional)

══════════════════════════════════════════════════════════════════
  TROUBLESHOOTING
══════════════════════════════════════════════════════════════════

  App won't start?
    → Make sure Java 17+ is installed: java --version (in CMD)

  AI offline indicator (red dot)?
    → The Python service may still be starting. Wait 5 seconds.

  "Python not found"?
    → Reinstall Python and tick "Add Python to PATH"

  Want to contribute or report a bug?
    → https://github.com/AmmarJamshed/smart-inventory-ai/issues

══════════════════════════════════════════════════════════════════
