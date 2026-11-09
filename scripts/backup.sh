#!/bin/bash
# ========================================================
# Database Backup Script for Hospital Appointment Platform
# Creates a timestamped PostgreSQL database dump on the host
# ========================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
BACKUP_DIR="${PROJECT_ROOT}/backups"
TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
BACKUP_FILE="${BACKUP_DIR}/hospital_db_backup_${TIMESTAMP}.sql"

mkdir -p "$BACKUP_DIR"

# Source environment variables if .env exists
if [ -f "${PROJECT_ROOT}/.env" ]; then
    export $(grep -v '^#' "${PROJECT_ROOT}/.env" | xargs)
fi

DB_USER="${DB_USER:-hospital_user}"
DB_NAME="${DB_NAME:-hospital_db}"
CONTAINER_NAME="hospital-postgres"

echo "=== Starting Database Backup ==="
echo "Target Container: ${CONTAINER_NAME}"
echo "Database: ${DB_NAME}"
echo "Output File: ${BACKUP_FILE}"

# Verify container is running
if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "ERROR: Container ${CONTAINER_NAME} is not running!" >&2
    exit 1
fi

# Execute pg_dump from container and stream directly to host file
docker exec -t "${CONTAINER_NAME}" pg_dump -U "${DB_USER}" -d "${DB_NAME}" --clean --if-exists > "${BACKUP_FILE}"

# Verify backup file size
if [ -s "${BACKUP_FILE}" ]; then
    FILESIZE=$(du -h "${BACKUP_FILE}" | cut -f1)
    echo "SUCCESS: Database backup completed! File size: ${FILESIZE}"
    echo "Backup location: ${BACKUP_FILE}"
else
    echo "ERROR: Backup file is empty!" >&2
    rm -f "${BACKUP_FILE}"
    exit 1
fi

# Rotate backups: Retain only the last 7 daily snapshots
find "${BACKUP_DIR}" -name "hospital_db_backup_*.sql" -type f -mtime +7 -delete || true
echo "Backup retention check completed."
