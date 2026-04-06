cmake_minimum_required(VERSION 3.15)
project(SmartInventoryEngine CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# ─── Include path for single-header libraries ──────────────────────────────
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/include)

# ─── Sources ───────────────────────────────────────────────────────────────
set(SOURCES
    src/main.cpp
    src/analytics.cpp
    src/matching.cpp
    src/reputation.cpp
)

add_executable(engine ${SOURCES})

# ─── Compiler flags ────────────────────────────────────────────────────────
if(MSVC)
    target_compile_options(engine PRIVATE /W3 /O2)
    target_compile_definitions(engine PRIVATE _WIN32_WINNT=0x0601)
else()
    target_compile_options(engine PRIVATE -Wall -O2)
endif()

# ─── Windows socket library (required by cpp-httplib) ─────────────────────
if(WIN32)
    target_link_libraries(engine PRIVATE ws2_32 mswsock)
endif()

# ─── Output directory ──────────────────────────────────────────────────────
set_target_properties(engine PROPERTIES
    RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}
    RUNTIME_OUTPUT_DIRECTORY_RELEASE ${CMAKE_BINARY_DIR}
    RUNTIME_OUTPUT_DIRECTORY_DEBUG ${CMAKE_BINARY_DIR}
)
