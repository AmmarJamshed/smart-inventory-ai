@echo off
title Download AI Model - TinyLlama 1.1B
color 0B
echo.
echo  ============================================================
echo   Download AI Model (TinyLlama 1.1B - Q4_K_M)
echo   Size: ~669 MB  |  License: Apache 2.0  |  Runs fully offline
echo  ============================================================
echo.
echo  This downloads the TinyLlama AI model to the models\ folder.
echo  Once downloaded, Smart Inventory AI uses it for intelligent
echo  NLP parsing WITHOUT any internet connection.
echo.
echo  If you skip this, the app still works using the built-in
echo  rule-based parser (English + Urdu supported).
echo.

set /p CONFIRM=Download now? (Y/N): 
if /i "%CONFIRM%" neq "Y" (
    echo  Skipped. Run SmartInventoryAI.bat to start the app.
    pause & exit /b 0
)

set SCRIPT_DIR=%~dp0
set MODEL_DIR=%SCRIPT_DIR%ai-service\models

python --version >nul 2>&1
if errorlevel 1 (
    echo  ERROR: Python required. Download from https://python.org
    pause & exit /b 1
)

echo.
echo  Installing huggingface-hub...
python -m pip install --quiet huggingface-hub

echo.
echo  Downloading model (~669 MB)...
python -c "
from huggingface_hub import hf_hub_download
import shutil, pathlib, sys

model_dir = pathlib.Path(r'%MODEL_DIR%')
model_dir.mkdir(parents=True, exist_ok=True)
dest = model_dir / 'tinyllama.gguf'

if dest.exists():
    print('  Model already downloaded at:', dest)
    sys.exit(0)

print('  Downloading... (this may take a few minutes)')
try:
    src = hf_hub_download(
        repo_id='TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF',
        filename='tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf',
        local_dir=str(model_dir),
        local_dir_use_symlinks=False,
    )
    shutil.copy(src, dest)
    size_mb = round(dest.stat().st_size / 1048576, 1)
    print(f'  Model ready! Size: {size_mb} MB')
    print(f'  Saved to: {dest}')
except Exception as e:
    print(f'  ERROR: {e}')
    sys.exit(1)
"

echo.
echo  ============================================================
echo   Model ready! Now run SmartInventoryAI.bat to start the app.
echo   The AI will automatically use the local model for parsing.
echo  ============================================================
echo.
pause
