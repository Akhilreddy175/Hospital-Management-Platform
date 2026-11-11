#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
BACKUP_DIR="${PROJECT_ROOT}/backups"
TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
BACKUP_FILE="${BACKUP_DIR}/hospital_db_backup_${TIMESTAMP}.sql"

mkdir -p "$BACKUP_DIR"

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

if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "ERROR: Container ${CONTAINER_NAME} is not running!" >&2
    exit 1
fi

docker exec -t "${CONTAINER_NAME}" pg_dump -U "${DB_USER}" -d "${DB_NAME}" --clean --if-exists > "${BACKUP_FILE}"

if [ -s "${BACKUP_FILE}" ]; then
    FILESIZE=$(du -h "${BACKUP_FILE}" | cut -f1)
    echo "SUCCESS: Database backup completed! File size: ${FILESIZE}"
    echo "Backup location: ${BACKUP_FILE}"
else
    echo "ERROR: Backup file is empty!" >&2
    rm -f "${BACKUP_FILE}"
    exit 1
fi

find "${BACKUP_DIR}" -name "hospital_db_backup_*.sql" -type f -mtime +7 -delete || true
echo "Backup retention check completed."
