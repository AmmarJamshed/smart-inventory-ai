# Smart Inventory AI

**Turn messages into insights — offline-first AI-powered inventory + trading system for restaurants & retailers.**

---

## System Architecture

```
┌─────────────────┐    REST     ┌──────────────────┐
│   JavaFX UI     │ ──────────► │ Python FastAPI    │  :8000
│   (Frontend)    │             │ (AI/NLP + Ollama) │
└─────────────────┘             └──────────────────┘
         │                               │
         │ REST                          │ SQLite
         ▼                               ▼
┌─────────────────┐    REST     ┌──────────────────┐
│ C++ REST Engine │ ◄────────── │  SQLite Database  │
│ (Analytics +    │             │  inventory.db     │
│  Matching)      │  :8080      └──────────────────┘
└─────────────────┘
```

## Prerequisites

| Tool         | Version  | Download                              |
|-------------|----------|---------------------------------------|
| Java JDK    | 17+      | https://adoptium.net                  |
| Maven       | 3.8+     | https://maven.apache.org              |
| Python      | 3.10+    | https://python.org                    |
| CMake       | 3.15+    | https://cmake.org                     |
| MinGW-w64   | Latest   | https://winlibs.com                   |
| Ollama      | Latest   | https://ollama.ai                     |

---

## Quick Start (Windows)

### Step 1 — Setup (run once)
```batch
cd D:\SmartInventoryAI
setup.bat
```

### Step 2 — Pull Ollama Model (run once)
```batch
ollama pull mistral
```

### Step 3 — Compile C++ Engine
```batch
compile_cpp.bat
```

### Step 4 — Start All Services
```batch
start_all.bat
```
This opens 3 terminal windows:
- AI Service (Python, port 8000)
- C++ Engine (port 8080)
- JavaFX App

---

## Manual Start (individual services)

### Python AI Service
```batch
cd D:\SmartInventoryAI\ai-service
venv\Scripts\activate
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

### C++ Engine
```batch
D:\SmartInventoryAI\cpp-engine\build\engine.exe
```

### JavaFX App
```batch
cd D:\SmartInventoryAI\frontend
mvn javafx:run
```

---

## Features

| Feature                  | Description                                      |
|--------------------------|--------------------------------------------------|
| NLP Inventory Entry      | Type natural language in English or Urdu         |
| Local AI (Ollama)        | Runs fully offline, no cloud APIs                |
| Inventory Table          | Live table with type badges + waste highlighting |
| Analytics Charts         | Pie, bar, line charts for insights               |
| B2B Trade Matching       | Privacy-preserving buyer/seller matching         |
| Dispute Resolution       | SHA-256 hashed transaction records               |
| Reputation Scoring       | 5-factor weighted reputation algorithm           |
| Dark Mode                | Toggle from sidebar                              |

---

## NLP Examples

```
English:  "Bought 10kg chicken @ 650/kg from market"
Urdu mix: "Aaj 2kg chicken zaya hogaya"
Urdu:     "5 dozen anday kharida 180 rupay dozen"
English:  "Sold 3kg beef to customer at 900/kg"
English:  "Transfer 5kg mutton to branch 2"
```

---

## API Reference

### Python AI Service (port 8000)
```
POST /parse           - Parse natural language text
GET  /health          - Service health check
GET  /inventory       - Get all inventory items
POST /transaction     - Save a transaction
```

### C++ Engine (port 8080)
```
POST /analytics       - Calculate inventory analytics
POST /match           - Find buyer/seller matches
POST /reputation      - Calculate reputation score
POST /recommendations - Get AI recommendations
GET  /health          - Engine health check
```

---

## Privacy System

Each user has:
- **Real identity** — encrypted, never exposed in UI
- **Pseudonym** — public display name (e.g., "Merchant #042")
- **DID** — decentralized identifier for signing

---

## Dispute Flow

1. Transaction created → SHA-256 hash generated
2. Hash stored in `transaction_hashes` table
3. User raises dispute with evidence
4. Admin resolves via dispute panel
5. Resolution recorded immutably

---

## Project Structure

```
D:\SmartInventoryAI\
├── README.md
├── setup.bat               ← Run first
├── compile_cpp.bat         ← Build C++ engine
├── start_all.bat           ← Start everything
├── database\
│   └── schema.sql
├── ai-service\             ← Python FastAPI
│   ├── main.py
│   ├── nlp_parser.py
│   ├── db_handler.py
│   └── requirements.txt
├── cpp-engine\             ← C++ Analytics Engine
│   ├── CMakeLists.txt
│   ├── include\            ← httplib.h, json.hpp (downloaded by setup.bat)
│   └── src\
│       ├── main.cpp
│       ├── analytics.cpp/h
│       ├── matching.cpp/h
│       └── reputation.cpp/h
├── frontend\               ← JavaFX Application
│   ├── pom.xml
│   └── src\main\
│       ├── java\com\smartinventory\
│       │   ├── App.java
│       │   ├── controller\MainController.java
│       │   ├── model\*.java
│       │   └── service\*.java
│       └── resources\com\smartinventory\
│           ├── main.fxml
│           └── css\styles.css
└── data\
    └── inventory.db        ← Generated at runtime
```
