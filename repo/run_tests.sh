#!/bin/bash
set -e

# Prevent Git Bash / MSYS from mangling paths passed to docker
export MSYS_NO_PATHCONV=1

echo "=========================================="
echo " DispatchOps Test Suite (Docker-based)"
echo "=========================================="

MAVEN_IMAGE="maven:3.9-eclipse-temurin-17"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

# ---------------------------------------------------------
# Step 1: Unit + Integration tests in Docker container
# ---------------------------------------------------------
echo ""
echo "[1/3] Running Unit + Integration Tests in Docker..."

docker run --rm \
    -v "${PROJECT_DIR}:/app" \
    -v dispatchops-maven-cache:/root/.m2 \
    -w /app \
    "${MAVEN_IMAGE}" \
    mvn test -DfailIfNoTests=false -B 2>&1 | tail -30

UNIT_EXIT=${PIPESTATUS[0]}
if [ "$UNIT_EXIT" -ne 0 ]; then
    echo ""
    echo "Unit/Integration tests FAILED (exit code $UNIT_EXIT)"
    exit $UNIT_EXIT
fi
echo "Unit + Integration tests PASSED"

# ---------------------------------------------------------
# Step 2: Start full app stack and run smoke check
# docker-compose.yml has built-in defaults for all secrets
# ---------------------------------------------------------
echo ""
echo "[2/3] Starting Application Stack..."

docker compose up -d --build --wait

# Health-check loop
RETRIES=30
until curl -sf http://localhost:8080/api/health > /dev/null 2>&1 || [ "$RETRIES" -eq 0 ]; do
    echo "  Waiting for app to be ready... ($RETRIES)"
    RETRIES=$((RETRIES - 1))
    sleep 2
done

if [ "$RETRIES" -eq 0 ]; then
    echo "ERROR: Application did not become healthy in time."
    docker compose logs app
    docker compose down -v
    exit 1
fi

echo "  Health check PASSED"

# Quick smoke test: verify /api/health responds
curl -sf http://localhost:8080/api/health | grep -q '"code":200' || {
    echo "FAIL: /api/health did not return expected payload"
    docker compose down -v
    exit 1
}
echo "  /api/health smoke check PASSED"

# ---------------------------------------------------------
# Step 3: Run the Playwright E2E suite explicitly against the
#         live Docker stack. The e2e/ module has its own pom.xml;
#         we invoke it via `-f e2e/pom.xml` so this step is
#         unambiguous in CI logs.
#
#         Playwright Java downloads Chromium automatically on
#         first use but needs system libraries; we install them
#         inside the Maven container before running tests. On
#         Linux hosts we use --network host so the container can
#         reach the app on localhost:8080; on Docker Desktop
#         (macOS/Windows) we use host.docker.internal.
# ---------------------------------------------------------
echo ""
echo "[3/3] Running Playwright E2E Suite against live stack..."

# Pick the right network hook for the host OS.
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    E2E_NETWORK_ARGS="--network host"
    E2E_URL="http://localhost:8080"
else
    E2E_NETWORK_ARGS="--add-host=host.docker.internal:host-gateway"
    E2E_URL="http://host.docker.internal:8080"
fi

docker run --rm \
    ${E2E_NETWORK_ARGS} \
    -v "${PROJECT_DIR}:/app" \
    -v dispatchops-maven-cache:/root/.m2 \
    -v dispatchops-playwright-cache:/root/.cache/ms-playwright \
    -w /app \
    -e E2E_BASE_URL="${E2E_URL}" \
    -e PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1 \
    "${MAVEN_IMAGE}" \
    bash -c "
set -e
echo '  [e2e] installing Chromium runtime dependencies (Ubuntu 24.04 package names)...'
apt-get update -qq >/dev/null
DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends -qq \
    libglib2.0-0t64 libnss3 libnspr4 libdbus-1-3 libatk1.0-0t64 libatk-bridge2.0-0t64 \
    libatspi2.0-0t64 libx11-6 libxcomposite1 libxdamage1 libxext6 libxfixes3 \
    libxrandr2 libgbm1 libxcb1 libxkbcommon0 libasound2t64 \
    libcups2t64 libdrm2 libpango-1.0-0 libcairo2 libxi6 libxtst6 \
    ca-certificates fonts-liberation >/dev/null 2>&1 || true
# Pre-install Chromium only (Playwright's auto-install tries all browsers
# including WebKit which is flaky to download on some networks). This call
# is idempotent — the shared playwright cache volume means the binary is
# downloaded once per CI machine and reused across runs.
if [ ! -d /root/.cache/ms-playwright/chromium-* ]; then
    echo '  [e2e] pre-installing Chromium (first run on this cache volume)...'
    unset PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD
    (cd /app/e2e && mvn -B org.codehaus.mojo:exec-maven-plugin:3.5.0:java \
        -Dexec.mainClass=com.microsoft.playwright.CLI \
        -Dexec.args='install chromium' 2>&1 | tail -15) || true
    export PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1
fi
echo '  [e2e] running mvn test ...'
cd /app/e2e && mvn test -B
" 2>&1 | tail -120

E2E_EXIT=${PIPESTATUS[0]}

# Tear down stack (volumes removed — ephemeral)
docker compose down -v

if [ "$E2E_EXIT" -ne 0 ]; then
    echo ""
    echo "E2E tests FAILED (exit code $E2E_EXIT)"
    exit $E2E_EXIT
fi
echo "E2E tests PASSED"

echo ""
echo "=========================================="
echo " ALL TESTS PASSED"
echo "=========================================="
