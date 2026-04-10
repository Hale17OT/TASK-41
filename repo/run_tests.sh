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
# Step 2: E2E tests against live Docker stack
# docker-compose.yml has built-in defaults for all secrets
# ---------------------------------------------------------
echo ""
echo "[2/2] Running E2E Tests against Docker stack..."

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

# Run Playwright E2E tests in Docker container
echo "  Running Playwright E2E suite..."
docker run --rm \
    --network host \
    -v "${PROJECT_DIR}/e2e:/app" \
    -v dispatchops-maven-cache:/root/.m2 \
    -w /app \
    -e E2E_BASE_URL=http://localhost:8080 \
    "${MAVEN_IMAGE}" \
    bash -c "mvn test -B 2>&1" | tail -40

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
echo " TEST RUN COMPLETE"
echo "=========================================="
