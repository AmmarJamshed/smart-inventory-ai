@echo off
title Smart Inventory AI
color 0A
echo.
echo  ============================================
echo   Smart Inventory AI v1.0.0
echo   Offline-first AI Inventory + Trade System
echo  ============================================
echo.

REM ── Check Java ───────────────────────────────────────────────────────────────
java --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java 17+ is required.
    echo Download from: https://adoptium.net
    pause & exit /b 1
)

REM ── Check Python ─────────────────────────────────────────────────────────────
python --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Python 3.10+ is required.
    echo Download from: https://python.org
    pause & exit /b 1
)

REM ── Start Python AI Service ───────────────────────────────────────────────────
echo [1/2] Starting AI Service (port 8000)...
set SCRIPT_DIR=%~dp0
set AI_DIR=%SCRIPT_DIR%..\ai-service

if not exist "%AI_DIR%\venv\Scripts\python.exe" (
    echo  Setting up Python environment...
    python -m venv "%AI_DIR%\venv"
    "%AI_DIR%\venv\Scripts\pip" install --quiet fastapi uvicorn pydantic requests
)

start "AI Service" /MIN cmd /c ""%AI_DIR%\venv\Scripts\python.exe" -m uvicorn main:app --host 0.0.0.0 --port 8000 --app-dir "%AI_DIR%""
timeout /t 3 /nobreak >nul

REM ── Launch JavaFX App ─────────────────────────────────────────────────────────
echo [2/2] Launching Smart Inventory AI...
java --module-path "%SCRIPT_DIR%javafx-sdk\lib" ^
     --add-modules javafx.controls,javafx.fxml,javafx.graphics ^
     --add-opens java.base/java.lang=ALL-UNNAMED ^
     -jar "%SCRIPT_DIR%smart-inventory-ai-1.0.0-fat.jar"

pause
