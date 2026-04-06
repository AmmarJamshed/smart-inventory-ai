@echo off
title Smart Inventory AI - Setup
color 0A
echo.
echo  ============================================
echo   Smart Inventory AI - First Time Setup
echo  ============================================
echo.

REM ─── Check Python ───────────────────────────────────────────────────────────
echo [1/5] Checking Python...
python --version >nul 2>&1
if errorlevel 1 (
    echo  ERROR: Python not found. Install from https://python.org
    pause & exit /b 1
)
python --version
echo  OK

REM ─── Python Virtual Environment ─────────────────────────────────────────────
echo.
echo [2/5] Creating Python virtual environment...
cd /d D:\SmartInventoryAI\ai-service
if exist venv (
    echo  venv already exists, skipping...
) else (
    python -m venv venv
    echo  Created venv
)
call venv\Scripts\activate.bat
echo  Installing Python packages...
pip install --quiet --upgrade pip
pip install --quiet -r requirements.txt
echo  Python setup complete
cd /d D:\SmartInventoryAI

REM ─── Download C++ Headers ────────────────────────────────────────────────────
echo.
echo [3/5] Downloading C++ single-header libraries...
cd /d D:\SmartInventoryAI\cpp-engine\include

if exist httplib.h (
    echo  httplib.h already exists, skipping...
) else (
    echo  Downloading cpp-httplib...
    powershell -Command "try { Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/yhirose/cpp-httplib/master/httplib.h' -OutFile 'httplib.h' -UseBasicParsing; Write-Host ' httplib.h downloaded' } catch { Write-Host ' WARNING: Could not download httplib.h - check internet connection' }"
)

if exist json.hpp (
    echo  json.hpp already exists, skipping...
) else (
    echo  Downloading nlohmann/json...
    powershell -Command "try { Invoke-WebRequest -Uri 'https://github.com/nlohmann/json/releases/download/v3.11.3/json.hpp' -OutFile 'json.hpp' -UseBasicParsing; Write-Host ' json.hpp downloaded' } catch { Write-Host ' WARNING: Could not download json.hpp - check internet connection' }"
)
cd /d D:\SmartInventoryAI

REM ─── Initialize Database ─────────────────────────────────────────────────────
echo.
echo [4/5] Initializing database...
cd /d D:\SmartInventoryAI\ai-service
call venv\Scripts\activate.bat
python -c "from db_handler import init_db; init_db(); print(' Database initialized')"
cd /d D:\SmartInventoryAI

REM ─── Check Maven ─────────────────────────────────────────────────────────────
echo.
echo [5/5] Checking Maven...
mvn --version >nul 2>&1
if errorlevel 1 (
    echo  WARNING: Maven not found. Install from https://maven.apache.org
    echo  You can still run Python and C++ services without Maven.
) else (
    echo  Maven found
    echo  Downloading Maven dependencies (may take a moment)...
    cd /d D:\SmartInventoryAI\frontend
    mvn dependency:resolve --quiet 2>nul
    echo  Maven dependencies ready
    cd /d D:\SmartInventoryAI
)

echo.
echo  ============================================
echo   Setup Complete!
echo  ============================================
echo.
echo  Next steps:
echo    1. Run: ollama pull mistral    (if Ollama installed)
echo    2. Run: compile_cpp.bat        (requires MinGW/MSVC)
echo    3. Run: start_all.bat          (start everything)
echo.
pause
