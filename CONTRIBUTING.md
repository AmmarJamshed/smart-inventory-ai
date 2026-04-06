# Contributing to Smart Inventory AI

Thank you for your interest in contributing! This is an open-source project.

## Architecture

```
JavaFX (Java 17+)  ──REST──►  Python FastAPI (port 8000)
        │                              │ SQLite DB
        └──REST──►  C++ Engine (port 8080)
```

## Development Setup

```bash
git clone https://github.com/YOUR_USERNAME/smart-inventory-ai
cd smart-inventory-ai
setup.bat        # Install dependencies
compile_cpp.bat  # Build C++ engine
start_all.bat    # Launch everything
```

## Areas to Contribute

| Area              | Language  | What to improve                              |
|-------------------|-----------|----------------------------------------------|
| NLP Parser        | Python    | Better Urdu support, more vocabulary         |
| Matching Engine   | C++       | Distance calculation with real GPS coords    |
| UI/UX             | JavaFX    | More charts, better animations               |
| Blockchain        | Any       | Replace SHA-256 mock with real chain         |
| Mobile            | Any       | React Native or Flutter companion app        |
| OCR Integration   | Python    | Parse from camera/image (EasyOCR)            |

## Coding Standards

- **Python**: PEP 8, type hints on all functions
- **Java**: Google Java Style, Javadoc on public methods
- **C++**: C++17, snake_case for functions, PascalCase for structs

## Adding New NLP Vocabulary

Edit `ai-service/nlp_parser.py`:
```python
ITEM_VOCAB = [
    "your_new_item",  # Add here
    ...
]
TYPE_KEYWORDS["bought"].append("your_urdu_word")
```

## Pull Request Process

1. Fork the repo
2. Create branch: `git checkout -b feature/your-feature`
3. Test with `mvn compile` and Python syntax check
4. Submit PR with description of changes

## License

MIT License — free to use, modify, and distribute.
