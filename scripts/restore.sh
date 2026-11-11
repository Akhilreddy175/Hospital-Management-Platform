#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

if [ -f "${PROJECT_ROOT}/.env" ]; then
    export $(grep -v '^#' "${PROJECT_ROOT}/.env" | xargs)
fi

DB_USER="${DB_USER:-hospital_user}"
DB_NAME="${DB_NAME:-hospital_db}"
CONTAINER_NAME="hospital-postgres"

if [ $# -lt 1 ]; then
    echo "Usage: $0 <path_to_backup_file.sql>"
    echo "Example: $0 ./backups/hospital_db_backup_20261109_120000.sql"
    exit 1
fi

RESTORE_FILE="$1"

if [ ! -f "${RESTORE_FILE}" ]; then
    echo "ERROR: Backup file ${RESTORE_FILE} does not exist!" >&2
    exit 1
fi

echo "=== Starting Database Restore ==="
echo "Target Container: ${CONTAINER_NAME}"
echo "Database: ${DB_NAME}"
echo "Source Backup: ${RESTORE_FILE}"

read -p "WARNING: This will overwrite data in '${DB_NAME}'. Continue? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Restore cancelled by user."
    exit 0
fi

cat "${RESTORE_FILE}" | docker exec -i "${CONTAINER_NAME}" psql -U "${DB_USER}" -d "${DB_NAME}"

echo "SUCCESS: Database successfully restored from ${RESTORE_FILE}!"
