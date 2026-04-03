#!/bin/bash

# Agent Skills - Quick Start Script
# This script helps you set up and run the OpenMMO AI backend with Agent Skills

set -e  # Exit on error

echo "🚀 OpenMMO AI Backend - Agent Skills Setup"
echo "=========================================="
echo ""

# Check prerequisites
echo "📋 Checking prerequisites..."

if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install Java 21+"
    exit 1
fi

if ! command -v gradle &> /dev/null && [ ! -f "./gradlew" ]; then
    echo "❌ Gradle is not found. Please ensure ~/.gradle/wrapper exists"
    exit 1
fi

echo "✅ Java found: $(java -version 2>&1 | head -n 1)"
echo ""

# Setup environment
echo "🔧 Setting up environment..."

if [ ! -f ".env.local" ]; then
    echo "📝 Creating .env.local from .env.example..."
    cp .env.example .env.local
    echo "⚠️  Please edit .env.local and add your API keys:"
    echo "   - OpenAI API Key: https://platform.openai.com/api-keys"
    echo "   - Or Anthropic API Key: https://console.anthropic.com"
    echo ""
else
    echo "✅ .env.local already exists"
fi

# Check API key
if [ -z "$OPENAI_API_KEY" ] && ! grep -q "OPENAI_API_KEY=sk-" .env.local 2>/dev/null; then
    echo "⚠️  WARNING: No OpenAI API key found in .env.local"
    echo "   Define OPENAI_API_KEY environment variable to continue."
    read -p "Press Enter to continue anyway, or Ctrl+C to exit..."
fi

echo ""
echo "📦 Building the project..."
echo ""

if [ -f "./gradlew" ]; then
    ./gradlew build -x test
else
    gradle build -x test
fi

echo ""
echo "✅ Build complete!"
echo ""

echo "🎯 Agent Skills Configured:"
echo "   ✓ code-reviewer     - Code analysis and review"
echo "   ✓ business-advisor  - Business strategy recommendations"
echo "   ✓ mmo-expert        - MMO game design expertise"
echo ""

echo "🚀 Starting the application..."
echo "   Server: http://localhost:8080"
echo "   Agent Skills Directory: .claude/skills/"
echo ""

if [ -f "./gradlew" ]; then
    ./gradlew bootRun
else
    gradle bootRun
fi
