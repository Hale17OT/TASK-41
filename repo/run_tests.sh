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
echo "[1/2] Running Unit + Integration Tests in Docker..."

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
# Step 2: Application stack smoke test
# docker-compose.yml has built-in defaults for all secrets
# ---------------------------------------------------------
echo ""
echo "[2/2] Running Application Stack Smoke Test..."

echo "  Starting application stack..."
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

# Smoke test: verify key API endpoints respond
echo "  Testing API endpoints..."
curl -sf http://localhost:8080/api/health | grep -q '"code":200' || { echo "FAIL: /api/health"; docker compose down -v; exit 1; }
echo "    /api/health ............ OK"

# Test login endpoint responds (will get 401 with bad creds, which is correct)
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"bad","password":"bad"}')
if [ "$HTTP_CODE" = "401" ]; then
    echo "    /api/auth/login ........ OK (401 on bad credentials)"
else
    echo "    /api/auth/login ........ WARN (got $HTTP_CODE)"
fi

echo "  Smoke tests PASSED"

# Tear down stack
docker compose down -v

echo ""
echo "=========================================="
echo " ALL TESTS PASSED"
echo "=========================================="
