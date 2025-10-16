#!/bin/bash

echo "==========================================="
echo "Modular Monolithic POC - Quick Start"
echo "==========================================="
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

echo "✅ Docker is running"
echo ""

# Clean up any existing containers
echo "🧹 Cleaning up existing containers..."
docker-compose down -v

echo ""
echo "🚀 Starting application..."
docker-compose up --build

# Note: This script will keep running and show logs
# Press Ctrl+C to stop

