#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"

if [ $# -lt 1 ]; then
    echo "Usage: $0 <target_image_tag>"
    echo "Example: $0 v1.0.0 (or a specific commit SHA)"
    exit 1
fi

TARGET_TAG="$1"
ENV_FILE="${PROJECT_ROOT}/.env"

echo "=== Initiating Zero-Downtime Rollback ==="
echo "Target Rollback Version: ${TARGET_TAG}"

if [ ! -f "${ENV_FILE}" ]; then
    echo "ERROR: .env file not found in ${PROJECT_ROOT}" >&2
    exit 1
fi

CURRENT_TAG=$(grep "^IMAGE_TAG=" "${ENV_FILE}" | cut -d '=' -f2 || echo "unknown")
echo "Current Version: ${CURRENT_TAG}"

if grep -q "^IMAGE_TAG=" "${ENV_FILE}"; then
    sed -i "s/^IMAGE_TAG=.*/IMAGE_TAG=${TARGET_TAG}/" "${ENV_FILE}"
else
    echo "IMAGE_TAG=${TARGET_TAG}" >> "${ENV_FILE}"
fi

echo "Pulling target image tag..."
docker compose pull api || echo "Note: Using local image if not in remote registry"

echo "Restarting API container with target image tag..."
docker compose up -d --no-deps api

echo "Waiting for health check verification..."
sleep 10

if docker inspect --format='{{json .State.Health.Status}}' hospital-api | grep -q "healthy"; then
    echo "SUCCESS: Rollback to ${TARGET_TAG} completed successfully and service is healthy!"
else
    echo "WARNING: Rollback completed but container is not yet reporting healthy. Check logs: docker compose logs api"
fi
