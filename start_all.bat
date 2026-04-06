@echo off
title Smart Inventory AI - Launcher
color 0A
echo.
echo  ============================================
echo   Starting Smart Inventory AI
echo  ============================================
echo.

REM ─── Start Python AI Service ────────────────────────────────────────────────
echo  Starting Python AI Service (port 8000)...
start "AI Service - Python" cmd /k "cd /d D:\SmartInventoryAI\ai-service && call venv\Scripts\activate && echo AI Service starting on http://localhost:8000 && uvicorn main:app --host 0.0.0.0 --port 8000"

REM Wait a moment for Python to start
timeout /t 3 /nobreak >nul

REM ─── Start C++ Engine ────────────────────────────────────────────────────────
if exist D:\SmartInventoryAI\cpp-engine\build\engine.exe (
    echo  Starting C++ Engine (port 8080)...
    start "C++ Engine" cmd /k "cd /d D:\SmartInventoryAI\cpp-engine\build && echo C++ Engine starting on http://localhost:8080 && engine.exe"
    timeout /t 2 /nobreak >nul
) else (
    echo  WARNING: C++ engine not compiled. Run compile_cpp.bat first.
    echo  The app will still work, but analytics will be limited.
)

REM ─── Start JavaFX App ────────────────────────────────────────────────────────
echo  Starting JavaFX Application...
mvn --version >nul 2>&1
if errorlevel 1 (
    echo  ERROR: Maven not found. Cannot start JavaFX app.
    echo  Install Maven from https://maven.apache.org
    pause
) else (
    start "Smart Inventory AI - App" cmd /k "cd /d D:\SmartInventoryAI\frontend && mvn javafx:run"
)

echo.
echo  ============================================
echo   All services launched!
echo  ============================================
echo.
echo  Services:
echo    AI Service:  http://localhost:8000
echo    C++ Engine:  http://localhost:8080
echo    API Docs:    http://localhost:8000/docs
echo.
echo  Press any key to close this launcher...
pause >nul
