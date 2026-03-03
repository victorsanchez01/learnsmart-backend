#!/bin/bash

# scripts/run_postman_tests.sh
# Automates LearnSmart API testing using Newman (CLI Postman runner)

set -e

# Default variables
GATEWAY_URL=${GATEWAY_URL:-"http://localhost:8762"}
AI_SERVICE_URL=${AI_SERVICE_URL:-"http://localhost:8000"}
COLLECTION_PATH="docs/LearnSmart.postman_collection.json"

echo "🚀 Starting LearnSmart API Automated Tests..."
echo "📍 Gateway: $GATEWAY_URL"
echo "📍 AI Service: $AI_SERVICE_URL"
echo ""

# 1. Check if Newman is available via npx
if ! npx -y newman --version > /dev/null 2>&1; then
    echo "⚠️ Newman not found. It will be downloaded via npx..."
fi

# 2. Run the tests
echo "🏃 Running Newman tests..."
npx -y newman run "$COLLECTION_PATH" \
    --global-var "gateway_url=$GATEWAY_URL" \
    --global-var "ai_service_url=$AI_SERVICE_URL" \
    --reporters cli \
    --color off \
    "$@"

echo ""
echo "✅ Tests completed successfully!"
