#!/bin/bash
# ============================================
# WVP Standalone Start Script
# Launches: Nginx + ZLM + WVP in one container
# ============================================

set -e

echo "============================================"
echo "  WVP Standalone Container Starting..."
echo "============================================"

# --- 1. Generate Nginx config.js (frontend base URL) ---
if [ -f /etc/nginx/conf.d/config.js ]; then
    sed -i "s|http://10.10.1.124:18978|http://localhost:18978|g" /etc/nginx/conf.d/config.js
    echo "[nginx] Frontend config.js updated to use localhost"
fi

# --- 2. Generate ZLM config.ini from template (if mounted) ---
if [ -f /opt/media/config.ini.template ]; then
    if [ ! -f /opt/media/config.ini ] || ! grep -q "localhost:18978" /opt/media/config.ini 2>/dev/null; then
        cp /opt/media/config.ini.template /opt/media/config.ini
        echo "[zlm] Config initialized from template"
    fi
fi

# --- 3. Start Nginx (frontend + reverse proxy) ---
echo "[nginx] Starting Nginx on port 8080..."
nginx -g 'daemon off;' &
NGINX_PID=$!
sleep 1

# Verify Nginx started
if kill -0 $NGINX_PID 2>/dev/null; then
    echo "[nginx] Started successfully (PID: $NGINX_PID)"
else
    echo "[nginx] FAILED to start"
    exit 1
fi

# --- 4. Start ZLMediaKit ---
echo "[zlm] Starting ZLMediaKit..."
ZLM_BIN="/opt/media/bin/MediaServer"

# Check if ZLM binary exists
if [ ! -f "$ZLM_BIN" ]; then
    # Try alternative binary names
    for candidate in MediaServer zlmediakit media_server; do
        if [ -f "/opt/media/bin/$candidate" ]; then
            ZLM_BIN="/opt/media/bin/$candidate"
            echo "[zlm] Found binary: $ZLM_BIN"
            break
        fi
    done
fi

if [ ! -f "$ZLM_BIN" ]; then
    echo "[zlm] ERROR: MediaServer binary not found in /opt/media/bin/"
    echo "[zlm] Available files:"
    ls -la /opt/media/bin/
    exit 1
fi

# Start ZLM with config
ZLM_CONFIG="/opt/media/config.ini"
if [ -f "$ZLM_CONFIG" ]; then
    echo "[zlm] Using config: $ZLM_CONFIG"
else
    echo "[zlm] WARNING: config.ini not found, using defaults"
    ZLM_CONFIG=""
fi

$ZLM_BIN -c "$ZLM_CONFIG" -l 0 &
ZLM_PID=$!
sleep 2

# Verify ZLM started
if kill -0 $ZLM_PID 2>/dev/null; then
    echo "[zlm] Started successfully (PID: $ZLM_PID)"
else
    echo "[zlm] FAILED to start — checking logs..."
    ls -la /opt/media/log/ 2>/dev/null || true
    cat /opt/media/log/*.log 2>/dev/null | tail -20 || true
    exit 1
fi

# --- 5. Start WVP (Spring Boot) ---
echo "[wvp] Starting WVP (Spring Boot)..."

# Build Spring Boot command with environment variables
WVP_OPTS=""

# If application-standalone-docker.yml exists, use it
STANDALONE_DOCKER="/opt/wvp/config/application-standalone-docker.yml"
if [ -f "$STANDALONE_DOCKER" ]; then
    WVP_OPTS="$WVP_OPTS --spring.profiles.active=standalone,docker"
    echo "[wvp] Using profiles: standalone,docker"
else
    WVP_OPTS="$WVP_OPTS --spring.profiles.active=standalone"
    echo "[wvp] Using profile: standalone"
fi

# Java options for containerized deployment
JAVA_OPTS="${JAVA_OPTS:--Xms128m -Xmx512m}"

# Start WVP
java ${JAVA_OPTS} -jar /opt/wvp/wvp.jar \
    $WVP_OPTS \
    --server.port=${WVP_HTTP_PORT:-18978} \
    --sip.port=${SIP_PORT:-8116} \
    2>&1 | tee /opt/wvp/logs/wvp.log &
WVP_PID=$!

sleep 3

# Verify WVP started
if kill -0 $WVP_PID 2>/dev/null; then
    echo "[wvp] Started successfully (PID: $WVP_PID)"
else
    echo "[wvp] FAILED to start — check logs at /opt/wvp/logs/wvp.log"
    tail -30 /opt/wvp/logs/wvp.log 2>/dev/null || true
    exit 1
fi

echo "============================================"
echo "  All services started!"
echo "  WVP API:     http://localhost:18978"
echo "  Web Frontend: http://localhost:8080"
echo "  SIP Port:    ${SIP_PORT:-8116}/tcp,udp"
echo "============================================"

# --- Signal handling: graceful shutdown ---
shutdown() {
    echo ""
    echo "Shutting down gracefully..."
    kill $WVP_PID 2>/dev/null
    kill $ZLM_PID 2>/dev/null
    kill $NGINX_PID 2>/dev/null
    wait 2>/dev/null
    echo "All services stopped."
}

trap shutdown SIGTERM SIGINT SIGQUIT

# Wait for any process to exit
wait
