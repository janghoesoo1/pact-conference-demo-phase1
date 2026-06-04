#!/bin/bash
# Conference API Load Test
# Usage: ./performance-test/load-test.sh [base_url] [concurrent] [requests]

BASE_URL=${1:-http://localhost:8080}
CONCURRENT=${2:-10}
REQUESTS=${3:-100}

echo "=== Conference API Load Test ==="
echo "Base URL: $BASE_URL"
echo "Concurrent: $CONCURRENT"
echo "Total Requests: $REQUESTS"
echo ""

# Test 1: GET /api/sessions
echo "--- Test 1: GET /api/sessions ---"
ab -n $REQUESTS -c $CONCURRENT -H "Accept: application/json" "$BASE_URL/api/sessions/" 2>/dev/null | grep -E "Requests per second|Time per request|Failed requests"

echo ""

# Test 2: GET /api/attendees
echo "--- Test 2: GET /api/attendees ---"
ab -n $REQUESTS -c $CONCURRENT -H "Accept: application/json" "$BASE_URL/api/attendees/" 2>/dev/null | grep -E "Requests per second|Time per request|Failed requests"

echo ""

# Test 3: GET /api/proposals
echo "--- Test 3: GET /api/proposals ---"
ab -n $REQUESTS -c $CONCURRENT -H "Accept: application/json" "$BASE_URL/api/proposals/" 2>/dev/null | grep -E "Requests per second|Time per request|Failed requests"

echo ""

# Test 4: GET /api/v1/sessions vs /api/v2/sessions
echo "--- Test 4: GET /api/v1/sessions ---"
ab -n $REQUESTS -c $CONCURRENT -H "Accept: application/json" "$BASE_URL/api/v1/sessions/" 2>/dev/null | grep -E "Requests per second|Time per request|Failed requests"

echo ""
echo "--- Test 5: GET /api/v2/sessions ---"
ab -n $REQUESTS -c $CONCURRENT -H "Accept: application/json" "$BASE_URL/api/v2/sessions/" 2>/dev/null | grep -E "Requests per second|Time per request|Failed requests"

echo ""
echo "=== Load Test Complete ==="
