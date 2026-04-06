@echo off
title Smart Inventory AI - Compile C++ Engine
color 0B
echo.
echo  ============================================
echo   Compiling C++ Analytics Engine
echo  ============================================
echo.

cd /d D:\SmartInventoryAI\cpp-engine

REM Check if include headers exist
if not exist include\httplib.h (
    echo  ERROR: include\httplib.h not found!
    echo  Run setup.bat first to download dependencies.
    pause & exit /b 1
)
if not exist include\json.hpp (
    echo  ERROR: include\json.hpp not found!
    echo  Run setup.bat first to download dependencies.
    pause & exit /b 1
)

REM Try CMake + MinGW build
echo  Attempting CMake build...
cmake --version >nul 2>&1
if errorlevel 1 (
    echo  CMake not found. Trying direct g++ compilation...
    goto direct_compile
)

if not exist build mkdir build
cd build
cmake .. -G "MinGW Makefiles" -DCMAKE_BUILD_TYPE=Release >cmake_log.txt 2>&1
if errorlevel 1 (
    echo  CMake MinGW failed, trying Visual Studio generator...
    cmake .. -DCMAKE_BUILD_TYPE=Release >cmake_log.txt 2>&1
    if errorlevel 1 (
        cd ..
        goto direct_compile
    )
)
cmake --build . --config Release
if errorlevel 1 (
    cd ..
    goto direct_compile
)
cd ..
echo.
echo  SUCCESS: Engine compiled via CMake
echo  Output: cpp-engine\build\engine.exe
goto done

:direct_compile
echo.
echo  Trying direct g++ compilation...
g++ --version >nul 2>&1
if errorlevel 1 (
    echo  ERROR: g++ not found!
    echo  Install MinGW-w64 from: https://winlibs.com
    echo  Add to PATH: C:\mingw64\bin
    pause & exit /b 1
)

if not exist build mkdir build
g++ -std=c++17 -O2 -o build\engine.exe ^
    src\main.cpp src\analytics.cpp src\matching.cpp src\reputation.cpp ^
    -Iinclude ^
    -lws2_32 ^
    -DCPPHTTPLIB_OPENSSL_SUPPORT=0
if errorlevel 1 (
    echo  ERROR: Compilation failed!
    pause & exit /b 1
)
echo  SUCCESS: Engine compiled via g++
echo  Output: cpp-engine\build\engine.exe

:done
echo.
echo  ============================================
echo   Compilation Complete
echo  ============================================
echo.
pause
