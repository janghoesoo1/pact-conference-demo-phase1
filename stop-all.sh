#!/bin/bash
# Stop all conference services

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "=== Stopping Conference Services ==="
echo ""

if [ -f "$SCRIPT_DIR/.service-pids" ]; then
    PIDS=$(cat "$SCRIPT_DIR/.service-pids")
    echo "Stopping services with PIDs: $PIDS"
    for PID in $PIDS; do
        if kill -0 "$PID" 2>/dev/null; then
            kill "$PID" 2>/dev/null
            echo "  Stopped PID: $PID"
        else
            echo "  PID $PID not running (already stopped)"
        fi
    done
    rm "$SCRIPT_DIR/.service-pids"
    echo ""
    echo "All services stopped."
else
    echo "No .service-pids file found. Searching by port..."
    echo ""
    STOPPED=0
    for port in 8080 8081 8082 8083; do
        PID=$(lsof -ti:"$port" 2>/dev/null)
        if [ -n "$PID" ]; then
            echo "  Stopping process on port $port (PID: $PID)"
            kill "$PID" 2>/dev/null
            STOPPED=$((STOPPED + 1))
        else
            echo "  Port $port: no process found"
        fi
    done
    echo ""
    if [ "$STOPPED" -gt 0 ]; then
        echo "Stopped $STOPPED service(s)."
    else
        echo "No running services found."
    fi
fi

echo ""
echo "Done."
