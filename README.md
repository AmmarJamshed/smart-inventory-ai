# Smart Inventory AI

**Turn messages into insights — offline-first AI-powered inventory + trading system for restaurants & retailers.**

---

## Download & Run (No Install Needed)

### Step 1 — Download
**[⬇ SmartInventoryAI-v1.0.0-Windows.zip](https://github.com/AmmarJamshed/smart-inventory-ai/releases/latest)**

### Step 2 — Run
Extract the ZIP → double-click **`SmartInventoryAI.exe`**

> **That's it.** No Java. No Python. No internet. No accounts. Everything is built in.

**Minimum requirement:** Windows 10 or 11 (64-bit)

---

## What It Does

| Feature | Description |
|---------|-------------|
| **NLP Inventory Entry** | Type messages in English or Urdu — AI parses them instantly |
| **Analytics Dashboard** | Waste %, total value, daily trend charts |
| **B2B Trade Matching** | Find buyers/sellers with reputation scores & reasoning |
| **Dispute System** | SHA-256 tamper-proof transaction hashing |
| **100% Offline** | Your data never leaves your machine |
| **Dark Mode** | Easy on the eyes during night shifts |

---

## Example Messages

```
Bought 10kg chicken @ 650/kg
Aaj 2kg gosht zaya hogaya
Sold 5 dozen anday @ 180/dozen
Received 20kg aata from mandi
2 liter tel kharab hogaya
Transfer 5kg rice to branch
```

---

## Architecture (All Bundled in the EXE)

```
┌─────────────────────────────────────────────────┐
│              SmartInventoryAI.exe                │
│                                                  │
│  ┌──────────────┐   ┌────────────────────────┐  │
│  │  JavaFX 21   │   │   Java NLP Engine       │  │
│  │  UI + Charts │   │   (English + Urdu)      │  │
│  └──────┬───────┘   └───────────┬────────────┘  │
│         │                       │                │
│         └──────────┬────────────┘                │
│                    ▼                              │
│  ┌─────────────────────────────────────────┐     │
│  │  LocalEngineService (Java)              │     │
│  │  Analytics · Matching · Reputation      │     │
│  └───────────────────┬─────────────────────┘     │
│                      ▼                            │
│  ┌─────────────────────────────────────────┐     │
│  │  SQLite Database  (local file)          │     │
│  └─────────────────────────────────────────┘     │
│                                                  │
│  Bundled JRE 21 · No Python · No downloads       │
└─────────────────────────────────────────────────┘
```

---

## Build From Source

### Prerequisites
- JDK 17+ (JDK 21 recommended)
- Maven 3.8+

### Quick Build
```bash
git clone https://github.com/AmmarJamshed/smart-inventory-ai.git
cd smart-inventory-ai

# Run directly (requires JDK + Maven)
cd frontend
mvn javafx:run

# Build self-contained Windows EXE
cd ..
build_exe.bat
```

The `build_exe.bat` script:
1. Compiles the Java app with Maven
2. Bundles the JRE using `jpackage`
3. Outputs `SmartInventoryAI-v1.0.0-Windows.zip` (~44 MB)

---

## Project Structure

```
smart-inventory-ai/
├── frontend/                  # JavaFX app (everything you need)
│   ├── src/main/java/com/smartinventory/
│   │   ├── App.java           # JavaFX entry point
│   │   ├── Launcher.java      # jpackage shim
│   │   ├── controller/
│   │   │   └── MainController.java
│   │   ├── model/             # POJOs
│   │   └── service/
│   │       ├── NlpParser.java         # English + Urdu NLP (no Python)
│   │       ├── LocalEngineService.java # Analytics + matching + reputation
│   │       └── DatabaseService.java   # SQLite access
│   └── src/main/resources/
│       ├── main.fxml          # UI layout
│       └── css/styles.css     # Premium UI styles
├── database/
│   └── schema.sql             # SQLite schema
├── build_exe.bat              # One-click EXE builder
└── README.md
```

---

## Privacy & Data

- All data is stored **locally** in an SQLite file on your machine
- Traders are shown by **pseudonyms** only — real identities are never exposed
- Transactions are hashed with **SHA-256** for tamper-proof audit trails
- No cloud, no telemetry, no accounts

---

## License

MIT License — free to use, modify, and distribute.

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for how to add features or report bugs.
