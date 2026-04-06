@echo off
title Smart Inventory AI v1.0.0
color 0A
echo.
echo  ============================================================
echo   Smart Inventory AI v1.0.0
echo   Offline-First AI Inventory + B2B Trading System
echo  ============================================================
echo.

set SCRIPT_DIR=%~dp0

REM ── Java check ────────────────────────────────────────────────────────────
java --version >nul 2>&1
if errorlevel 1 (
    echo  ERROR: Java 17+ is required.
    echo  Download free from: https://adoptium.net
    echo  Install, then rerun this file.
    pause & exit /b 1
)

REM ── Python check ─────────────────────────────────────────────────────────
python --version >nul 2>&1
if errorlevel 1 (
    echo  ERROR: Python 3.10+ is required.
    echo  Download free from: https://python.org
    echo  Make sure to tick "Add Python to PATH" during install.
    pause & exit /b 1
)

REM ── Python dependencies ───────────────────────────────────────────────────
if not exist "%SCRIPT_DIR%ai-service\venv\Scripts\python.exe" (
    echo  Setting up Python environment (first run only, ~2 min)...
    python -m venv "%SCRIPT_DIR%ai-service\venv"
    "%SCRIPT_DIR%ai-service\venv\Scripts\pip" install --quiet fastapi uvicorn pydantic requests
    echo  Setup complete!
)

REM ── Start AI Service ──────────────────────────────────────────────────────
echo  [1/2] Starting AI Service on http://localhost:8000 ...
start "Smart Inventory AI - AI Service" /MIN cmd /c ^
    ""%SCRIPT_DIR%ai-service\venv\Scripts\python.exe" -m uvicorn main:app --host 0.0.0.0 --port 8000 --app-dir "%SCRIPT_DIR%ai-service" 2>&1"
timeout /t 4 /nobreak >nul

REM ── Launch JavaFX App ─────────────────────────────────────────────────────
echo  [2/2] Launching Smart Inventory AI...
java -jar "%SCRIPT_DIR%smart-inventory-ai.jar"

pause
