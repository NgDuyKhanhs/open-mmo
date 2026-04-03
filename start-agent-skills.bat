@echo off
REM Agent Skills - Quick Start Script (Windows)
REM This script helps you set up and run the OpenMMO AI backend with Agent Skills

setlocal enabledelayedexpansion

echo.
echo ========================================
echo 🚀 OpenMMO AI Backend - Agent Skills Setup
echo ========================================
echo.

REM Check prerequisites
echo 📋 Checking prerequisites...

where java >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo ❌ Java is not installed. Please install Java 21+
    exit /b 1
)

for /f "tokens=*" %%i in ('java -version 2^>^&1') do (
    set JAVA_VERSION=%%i
    goto :java_found
)

:java_found
echo ✅ Java found: %JAVA_VERSION%
echo.

REM Setup environment
echo 🔧 Setting up environment...

if not exist ".env.local" (
    echo 📝 Creating .env.local from .env.example...
    copy .env.example .env.local >nul
    echo ⚠️  Please edit .env.local and add your API keys:
    echo    - OpenAI API Key: https://platform.openai.com/api-keys
    echo    - Or Anthropic API Key: https://console.anthropic.com
    echo.
) else (
    echo ✅ .env.local already exists
)

echo.
echo 📦 Building the project...
echo.

if exist "gradlew.bat" (
    call gradlew.bat build -x test
) else (
    gradle build -x test
)

echo.
echo ✅ Build complete!
echo.

echo 🎯 Agent Skills Configured:
echo    ✓ code-reviewer     - Code analysis and review
echo    ✓ business-advisor  - Business strategy recommendations
echo    ✓ mmo-expert        - MMO game design expertise
echo.

echo 🚀 Starting the application...
echo    Server: http://localhost:8080
echo    Agent Skills Directory: .claude\skills\
echo.

if exist "gradlew.bat" (
    call gradlew.bat bootRun
) else (
    gradle bootRun
)

pause
