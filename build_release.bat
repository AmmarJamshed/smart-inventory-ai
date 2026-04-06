@echo off
title Smart Inventory AI - Build Release
color 0B
echo.
echo  ============================================
echo   Building Smart Inventory AI Release
echo  ============================================
echo.

set VERSION=1.0.0
set DIST_DIR=D:\SmartInventoryAI\release\SmartInventoryAI-%VERSION%

REM ── Clean previous release ────────────────────────────────────────────────
if exist "%DIST_DIR%" rmdir /s /q "%DIST_DIR%"
mkdir "%DIST_DIR%"
mkdir "%DIST_DIR%\ai-service"
mkdir "%DIST_DIR%\models"
mkdir "%DIST_DIR%\data"
echo  Created release directory: %DIST_DIR%

REM ── Build JavaFX fat JAR ──────────────────────────────────────────────────
echo.
echo [1/4] Building JavaFX application...
cd /d D:\SmartInventoryAI\frontend
call mvn package -DskipTests -q
if errorlevel 1 (
    echo  ERROR: Maven build failed!
    pause & exit /b 1
)
copy "target\smart-inventory-ai-%VERSION%-fat.jar" "%DIST_DIR%\smart-inventory-ai.jar"
echo  JavaFX JAR built: smart-inventory-ai.jar

REM ── Bundle Python AI Service with PyInstaller ─────────────────────────────
echo.
echo [2/4] Bundling Python AI service...
cd /d D:\SmartInventoryAI\ai-service

if not exist venv\Scripts\pyinstaller.exe (
    call venv\Scripts\pip install --quiet pyinstaller
)

call venv\Scripts\pyinstaller --onefile --name ai-service ^
    --hidden-import=fastapi ^
    --hidden-import=uvicorn ^
    --hidden-import=pydantic ^
    --hidden-import=llama_cpp ^
    --collect-all llama_cpp ^
    --add-data "nlp_parser.py;." ^
    --add-data "db_handler.py;." ^
    --add-data "local_model.py;." ^
    --distpath "%DIST_DIR%\ai-service" ^
    main.py

if errorlevel 1 (
    echo  WARNING: PyInstaller failed. Falling back to script mode.
    REM Copy Python scripts as fallback
    copy "main.py" "%DIST_DIR%\ai-service\"
    copy "nlp_parser.py" "%DIST_DIR%\ai-service\"
    copy "db_handler.py" "%DIST_DIR%\ai-service\"
    copy "local_model.py" "%DIST_DIR%\ai-service\"
    copy "requirements.txt" "%DIST_DIR%\ai-service\"
) else (
    echo  Python service bundled as EXE
)

REM ── Copy database schema & config ─────────────────────────────────────────
cd /d D:\SmartInventoryAI
copy "database\schema.sql" "%DIST_DIR%\"
echo  Copied database schema

REM ── Create launcher scripts ───────────────────────────────────────────────
echo.
echo [3/4] Creating launcher scripts...

REM Main launcher
(
echo @echo off
echo title Smart Inventory AI v%VERSION%
echo color 0A
echo echo.
echo echo  ============================================================
echo echo   Smart Inventory AI v%VERSION%
echo echo   Offline-First AI Inventory + B2B Trading System
echo echo  ============================================================
echo echo.
echo.
echo set SCRIPT_DIR=%%~dp0
echo.
echo REM Check Java
echo java --version ^>nul 2^>^&1
echo if errorlevel 1 (
echo     echo  ERROR: Java 17+ required. Download: https://adoptium.net
echo     pause ^& exit /b 1
echo ^)
echo.
echo REM Start AI Service
echo echo [1/2] Starting AI Service...
echo if exist "%%SCRIPT_DIR%%ai-service\ai-service.exe" (
echo     start "Smart Inventory AI Service" /MIN "%%SCRIPT_DIR%%ai-service\ai-service.exe"
echo ^) else (
echo     echo  WARNING: AI service EXE not found. Using Python...
echo     python "%%SCRIPT_DIR%%ai-service\main.py"
echo ^)
echo timeout /t 3 /nobreak ^>nul
echo.
echo REM Launch App
echo echo [2/2] Launching application...
echo java -jar "%%SCRIPT_DIR%%smart-inventory-ai.jar"
echo.
echo pause
) > "%DIST_DIR%\SmartInventoryAI.bat"

REM Download model script
(
echo @echo off
echo title Download AI Model
echo color 0A
echo echo.
echo echo  Downloading TinyLlama AI Model (~669 MB^)
echo echo  This is a ONE-TIME download. The model runs fully offline.
echo echo.
echo python -c "from huggingface_hub import hf_hub_download; import shutil, pathlib; p=pathlib.Path('models'); p.mkdir(exist_ok=True); src=hf_hub_download('TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF','tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf',local_dir=str(p),local_dir_use_symlinks=False); shutil.copy(src, p/'tinyllama.gguf'); print('Model ready!')"
echo echo.
echo echo  Model downloaded! Restart SmartInventoryAI.bat
echo pause
) > "%DIST_DIR%\download_model.bat"

echo  Launcher scripts created

REM ── Package as ZIP ────────────────────────────────────────────────────────
echo.
echo [4/4] Creating ZIP package...
cd /d D:\SmartInventoryAI\release
powershell -Command "Compress-Archive -Path '%DIST_DIR%' -DestinationPath 'SmartInventoryAI-%VERSION%-windows.zip' -Force"
echo  ZIP created: SmartInventoryAI-%VERSION%-windows.zip

echo.
echo  ============================================
echo   Build Complete!
echo  ============================================
echo.
echo  Release: D:\SmartInventoryAI\release\SmartInventoryAI-%VERSION%
echo  ZIP:     D:\SmartInventoryAI\release\SmartInventoryAI-%VERSION%-windows.zip
echo.
pause
