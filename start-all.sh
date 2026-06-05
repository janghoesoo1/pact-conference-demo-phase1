#!/bin/bash
# Start all conference services
# Usage: ./start-all.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "=== Conference Management System ==="
echo ""

# Check Java
if ! command -v java &>/dev/null; then
    echo "[ERROR] Java not found. Please install JDK 21."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -1 | awk -F '"' '{print $2}' | cut -d. -f1)
echo "Java version: $(java -version 2>&1 | head -1)"

# Build all
echo "[1/5] Building all modules..."
"$SCRIPT_DIR/gradlew" -p "$SCRIPT_DIR" clean bootJar -x test --parallel 2>&1 | tail -5
echo "Build complete."
echo ""

# Start services
echo "[2/5] Starting Session Service (port 8082)..."
java -jar "$SCRIPT_DIR/session-service/build/libs/"*.jar \
    --server.port=8082 \
    > /tmp/session-service.log 2>&1 &
SESSION_PID=$!
echo "  Session Service PID: $SESSION_PID"

echo "[3/5] Starting Attendee Service (port 8081)..."
java -jar "$SCRIPT_DIR/attendee-service/build/libs/"*.jar \
    --server.port=8081 \
    > /tmp/attendee-service.log 2>&1 &
ATTENDEE_PID=$!
echo "  Attendee Service PID: $ATTENDEE_PID"

echo "[4/5] Starting CFP Service (port 8083)..."
java -jar "$SCRIPT_DIR/cfp-service/build/libs/"*.jar \
    --server.port=8083 \
    > /tmp/cfp-service.log 2>&1 &
CFP_PID=$!
echo "  CFP Service PID: $CFP_PID"

echo "[5/5] Starting Gateway (port 8080)..."
java -jar "$SCRIPT_DIR/gateway/build/libs/"*.jar \
    --server.port=8080 \
    > /tmp/gateway.log 2>&1 &
GATEWAY_PID=$!
echo "  Gateway PID: $GATEWAY_PID"

echo ""
echo "=== All services starting ==="
echo ""

# Wait for startup (approximate)
echo "Waiting for services to start (20s)..."
sleep 5
echo "  [5s] Session service starting..."
sleep 5
echo "  [10s] Attendee service starting..."
sleep 5
echo "  [15s] CFP service starting..."
sleep 5
echo "  [20s] Gateway starting..."
echo ""

echo "================================================================"
echo " Services"
echo "================================================================"
echo "  Gateway:          http://localhost:8080"
echo "  Attendee Service: http://localhost:8081"
echo "  Session Service:  http://localhost:8082"
echo "  CFP Service:      http://localhost:8083"
echo ""
echo "================================================================"
echo " Swagger UI"
echo "================================================================"
echo "  Attendee: http://localhost:8081/swagger-ui.html"
echo "  Session:  http://localhost:8082/swagger-ui.html"
echo "  CFP:      http://localhost:8083/swagger-ui.html"
echo ""
echo "================================================================"
echo " Monitoring"
echo "================================================================"
echo "  Health:     http://localhost:8080/actuator/health"
echo "  Prometheus: http://localhost:8082/actuator/prometheus"
echo "  Pact Broker: http://localhost:9292 (pact/pact)"
echo ""
echo "================================================================"
echo " Dashboard"
echo "================================================================"
echo "  file://$SCRIPT_DIR/dashboard/index.html"
echo ""
echo "================================================================"
echo " Logs"
echo "================================================================"
echo "  Gateway:   tail -f /tmp/gateway.log"
echo "  Attendee:  tail -f /tmp/attendee-service.log"
echo "  Session:   tail -f /tmp/session-service.log"
echo "  CFP:       tail -f /tmp/cfp-service.log"
echo ""
echo "================================================================"
echo " PIDs"
echo "================================================================"
echo "  session=$SESSION_PID  attendee=$ATTENDEE_PID  cfp=$CFP_PID  gateway=$GATEWAY_PID"
echo "  To stop: ./stop-all.sh"
echo "================================================================"
echo ""

# Save PIDs for stop script
echo "$SESSION_PID $ATTENDEE_PID $CFP_PID $GATEWAY_PID" > "$SCRIPT_DIR/.service-pids"
echo "PID file saved: $SCRIPT_DIR/.service-pids"
echo ""

# Quick health check
echo "Health check..."
for port in 8082 8081 8083 8080; do
    if curl -s -o /dev/null -w "%{http_code}" "http://localhost:$port/actuator/health" 2>/dev/null | grep -q "200"; then
        echo "  :$port - OK"
    else
        echo "  :$port - Starting... (check log if this persists)"
    fi
done
echo ""

wait
