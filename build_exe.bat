@echo off
setlocal enabledelayedexpansion
title Smart Inventory AI — Build EXE

echo ============================================================
echo   Smart Inventory AI — Self-Contained Windows EXE Builder
echo ============================================================
echo.
echo  What this does:
echo   1. Compiles the Java app
echo   2. Packages it as a standalone Windows EXE
echo   3. Bundles the JRE so users need NO Java installed
echo   4. Zips everything into a single distributable archive
echo.

REM ── Paths ────────────────────────────────────────────────────────────────────
set ROOT=D:\SmartInventoryAI
set FRONT=%ROOT%\frontend
set DIST=%ROOT%\dist
set JFXVER=21.0.7
set M2=%USERPROFILE%\.m2\repository\org\openjfx
set APPVER=1.0.0

REM ── Check prerequisites ───────────────────────────────────────────────────────
echo [1/6] Checking prerequisites...

where java >nul 2>&1 || (echo ERROR: Java not found in PATH & pause & exit /b 1)
where mvn  >nul 2>&1 || (echo ERROR: Maven not found in PATH & pause & exit /b 1)

for /f "tokens=*" %%v in ('java -version 2^>^&1') do (
    echo        Java: %%v
    goto :java_ver_done
)
:java_ver_done

REM Check jpackage availability (JDK 14+)
where jpackage >nul 2>&1 || (
    echo ERROR: jpackage not found. Make sure you have JDK 14 or newer.
    echo        Run:  java -version  to check.
    pause & exit /b 1
)
echo        jpackage: OK
echo.

REM ── Step 1: Maven build ───────────────────────────────────────────────────────
echo [2/6] Building with Maven...
cd /d "%FRONT%"
call mvn clean package -DskipTests -q
if %ERRORLEVEL% neq 0 (
    echo ERROR: Maven build failed! Check target\ for errors.
    pause & exit /b 1
)
echo        Build: SUCCESS
echo.

REM ── Step 2: Copy dependencies to target\lib ───────────────────────────────────
echo [3/6] Collecting dependencies...
if not exist "%FRONT%\target\lib" mkdir "%FRONT%\target\lib"

call mvn dependency:copy-dependencies -DoutputDirectory="%FRONT%\target\lib" -q
if %ERRORLEVEL% neq 0 (
    echo ERROR: Dependency copy failed.
    pause & exit /b 1
)

REM Copy the app JAR into lib so jpackage finds everything in one dir
copy /Y "%FRONT%\target\smart-inventory-ai-%APPVER%.jar" "%FRONT%\target\lib\" >nul

REM Remove JavaFX JARs from lib — jpackage gets those from --module-path below
del /Q "%FRONT%\target\lib\javafx-*.jar" 2>nul

echo        Dependencies collected.
echo.

REM ── Step 3: Collect JavaFX platform JARs (contain Windows .dll natives) ──────
echo [4/6] Locating JavaFX native modules...
set JFXMODS=%M2%\javafx-base\%JFXVER%\javafx-base-%JFXVER%-win.jar
set JFXMODS=%JFXMODS%;%M2%\javafx-graphics\%JFXVER%\javafx-graphics-%JFXVER%-win.jar
set JFXMODS=%JFXMODS%;%M2%\javafx-controls\%JFXVER%\javafx-controls-%JFXVER%-win.jar
set JFXMODS=%JFXMODS%;%M2%\javafx-fxml\%JFXVER%\javafx-fxml-%JFXVER%-win.jar
set JFXMODS=%JFXMODS%;%M2%\javafx-media\%JFXVER%\javafx-media-%JFXVER%-win.jar

REM Verify at least one exists
if not exist "%M2%\javafx-base\%JFXVER%\javafx-base-%JFXVER%-win.jar" (
    echo.
    echo  JavaFX Win platform JARs not found in Maven cache.
    echo  Downloading via Maven...
    call mvn dependency:get -Dartifact=org.openjfx:javafx-base:%JFXVER%:jar:win -q
    call mvn dependency:get -Dartifact=org.openjfx:javafx-graphics:%JFXVER%:jar:win -q
    call mvn dependency:get -Dartifact=org.openjfx:javafx-controls:%JFXVER%:jar:win -q
    call mvn dependency:get -Dartifact=org.openjfx:javafx-fxml:%JFXVER%:jar:win -q
    call mvn dependency:get -Dartifact=org.openjfx:javafx-media:%JFXVER%:jar:win -q
)
echo        JavaFX modules: OK
echo.

REM ── Step 4: Create database dir for jpackage ──────────────────────────────────
if not exist "%FRONT%\target\lib\data" mkdir "%FRONT%\target\lib\data"

REM ── Step 5: Run jpackage ──────────────────────────────────────────────────────
echo [5/6] Running jpackage (bundles JRE — this takes ~60 seconds)...
if exist "%DIST%" rmdir /s /q "%DIST%"
mkdir "%DIST%"

jpackage ^
    --type app-image ^
    --name "SmartInventoryAI" ^
    --app-version "%APPVER%" ^
    --description "Offline AI-powered inventory and trading system for restaurants" ^
    --vendor "SmartInventoryAI" ^
    --input "%FRONT%\target\lib" ^
    --main-jar "smart-inventory-ai-%APPVER%.jar" ^
    --module-path "%JFXMODS%" ^
    --add-modules "javafx.controls,javafx.fxml,javafx.media,java.sql,java.desktop,java.xml,java.logging,java.naming" ^
    --java-options "--add-opens java.base/java.lang=ALL-UNNAMED" ^
    --java-options "--add-opens javafx.base/com.sun.javafx.reflect=ALL-UNNAMED" ^
    --java-options "--add-opens javafx.graphics/com.sun.javafx.application=ALL-UNNAMED" ^
    --java-options "-Dfile.encoding=UTF-8" ^
    --dest "%DIST%"

if %ERRORLEVEL% neq 0 (
    echo.
    echo ERROR: jpackage failed!
    echo.
    echo  Tip: Make sure you have JDK 17+ installed (not just JRE).
    echo  Check the error above for details.
    pause & exit /b 1
)

echo        jpackage: SUCCESS — app-image created at %DIST%\SmartInventoryAI
echo.

REM ── Step 6: Create database directory inside app-image ───────────────────────
if not exist "%DIST%\SmartInventoryAI\data" mkdir "%DIST%\SmartInventoryAI\data"

REM Copy schema if it exists
if exist "%ROOT%\database\schema.sql" (
    copy /Y "%ROOT%\database\schema.sql" "%DIST%\SmartInventoryAI\data\" >nul
)

REM Write a quick-start README
echo Smart Inventory AI v%APPVER% - Quick Start > "%DIST%\SmartInventoryAI\README.txt"
echo ======================================== >> "%DIST%\SmartInventoryAI\README.txt"
echo. >> "%DIST%\SmartInventoryAI\README.txt"
echo HOW TO RUN: >> "%DIST%\SmartInventoryAI\README.txt"
echo   Double-click SmartInventoryAI.exe >> "%DIST%\SmartInventoryAI\README.txt"
echo. >> "%DIST%\SmartInventoryAI\README.txt"
echo No installation needed. No Java required. No internet needed. >> "%DIST%\SmartInventoryAI\README.txt"
echo Everything is built in. >> "%DIST%\SmartInventoryAI\README.txt"
echo. >> "%DIST%\SmartInventoryAI\README.txt"
echo EXAMPLE MESSAGES TO TRY: >> "%DIST%\SmartInventoryAI\README.txt"
echo   "Bought 10kg chicken @ 650/kg" >> "%DIST%\SmartInventoryAI\README.txt"
echo   "Aaj 2kg gosht zaya hogaya" >> "%DIST%\SmartInventoryAI\README.txt"
echo   "Sold 5 dozen anday @ 180/dozen" >> "%DIST%\SmartInventoryAI\README.txt"
echo   "Received 20kg aata from supplier" >> "%DIST%\SmartInventoryAI\README.txt"

REM ── Step 7: Zip it ────────────────────────────────────────────────────────────
echo [6/6] Creating ZIP archive...
set ZIPNAME=SmartInventoryAI-v%APPVER%-Windows.zip
cd /d "%DIST%"
powershell -NoProfile -Command ^
    "Compress-Archive -Path 'SmartInventoryAI' -DestinationPath '..\%ZIPNAME%' -Force"

if %ERRORLEVEL% neq 0 (
    echo WARNING: ZIP creation failed but app-image is ready at %DIST%\SmartInventoryAI
) else (
    echo        ZIP: %ROOT%\%ZIPNAME%
)

echo.
echo ============================================================
echo   BUILD COMPLETE!
echo ============================================================
echo.
echo   App folder : %DIST%\SmartInventoryAI\
echo   EXE file   : %DIST%\SmartInventoryAI\SmartInventoryAI.exe
echo   ZIP file   : %ROOT%\%ZIPNAME%
echo.
echo   TEST IT NOW:
echo   "%DIST%\SmartInventoryAI\SmartInventoryAI.exe"
echo.
pause
