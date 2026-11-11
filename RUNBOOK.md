# Runbook — Hospital Management Platform

Operational reference for running and maintaining the Hospital Appointment API on Docker Compose (local or EC2).

All commands assume you are in the project root (`~/Hospital-Management-Platform` on EC2, or `C:\Users\gudap\Hospital Management Platform` locally).

---

## Starting and Stopping

**Start all containers in the background:**

```bash
docker compose up -d
```

Compose starts services in dependency order: `hospital-postgres` first, then `hospital-api` once Postgres reports healthy, then `hospital-nginx` once the API reports healthy. Full startup takes around 30–40 seconds.

**Stop all containers (data preserved):**

```bash
docker compose down
```

**Stop and delete the database volume (destructive — erases all data):**

```bash
docker compose down -v
```

---

## Checking Container Status and Health

**List containers and their health state:**

```bash
docker compose ps
```

All three containers should show `healthy` once the stack is running. Expected output:

```
NAME               STATUS
hospital-nginx     Up (healthy)
hospital-api       Up (healthy)
hospital-postgres  Up (healthy)
```

**Inspect the health status of a specific container:**

```bash
docker inspect --format='{{.State.Health.Status}}' hospital-api
```

**Hit the health endpoint directly:**

```bash
curl -k https://localhost/actuator/health
```

Expected: `{"status":"UP"}`. On EC2 replace `localhost` with the public IP.

---

## Viewing Logs

**All containers (follow):**

```bash
docker compose logs -f
```

**API only (Spring Boot, structured JSON):**

```bash
docker compose logs -f api
```

**Nginx access and error logs:**

```bash
docker compose logs -f nginx
```

**PostgreSQL logs:**

```bash
docker compose logs -f postgres
```

**Last N lines from a specific container:**

```bash
docker compose logs --tail=100 api
```

---

## Restarting Failed Services

**Restart a single service without touching others:**

```bash
docker compose restart api
docker compose restart nginx
docker compose restart postgres
```

**Force recreate the API container (pulls no new image):**

```bash
docker compose up -d --force-recreate --no-deps api
```

**Recreate everything:**

```bash
docker compose up -d --force-recreate
```

If the API container keeps crashing, check logs before restarting:

```bash
docker compose logs --tail=50 api
```

Common causes: missing `.env`, wrong `DB_PASSWORD`, or the database not ready.

---

## Database Backup

The `scripts/backup.sh` script runs `pg_dump` inside the `hospital-postgres` container and writes a timestamped SQL file to the `backups/` directory on the host.

```bash
chmod +x scripts/backup.sh
./scripts/backup.sh
```

Output:

```
=== Starting Database Backup ===
Target Container: hospital-postgres
Database: hospital_db
Output File: /path/to/backups/hospital_db_backup_20261110_161202.sql
SUCCESS: Database backup completed! File size: 16K
```

Backups older than 7 days are automatically deleted. Backup files are excluded from git via `.gitignore`.

**List existing backups:**

```bash
ls -lh backups/
```

---

## Restoring a Backup

The `scripts/restore.sh` script pipes a dump file back into the running `hospital-postgres` container using `psql`.

```bash
chmod +x scripts/restore.sh
./scripts/restore.sh ./backups/hospital_db_backup_20261110_161202.sql
```

The script will ask for confirmation before overwriting the database. Type `y` to proceed.

> **Note:** The API container does not need to be stopped before restoring. However, in-flight requests during restore may encounter inconsistent state. For a production restore, consider running `docker compose stop api` first, restoring, then `docker compose start api`.

---

## Rolling Back an Application Version

The `scripts/rollback.sh` script updates `IMAGE_TAG` in `.env`, pulls the specified image tag from GHCR, and restarts the `api` service with zero downtime.

```bash
chmod +x scripts/rollback.sh

# Roll back to the latest published image
./scripts/rollback.sh latest

# Roll back to a specific commit SHA
./scripts/rollback.sh sha-a1b2c3d4e5f6...
```

The script polls the container health check after restart and reports success or a warning if the container hasn't reached `healthy` within 10 seconds.

Available image tags are listed in the GHCR packages page at:  
`https://github.com/Akhilreddy175/Hospital-Management-Platform/pkgs/container/hospital-appointment-service`

---

## Updating to a New Version

After a successful CI/CD run pushes a new `:latest` image to GHCR:

```bash
docker compose pull api
docker compose up -d --no-deps api
```

The API container is replaced with the new image. The Postgres and Nginx containers are unaffected.

---

## Troubleshooting

### API container exits immediately

Check the last log entries:

```bash
docker compose logs --tail=30 api
```

Likely causes:
- **Database connection failed:** Confirm `hospital-postgres` is `healthy` and that `DB_HOST`, `DB_USER`, `DB_PASSWORD` in `.env` match the Postgres container environment.
- **Flyway migration error:** If the schema has changed on a live database, Flyway will refuse to start. Never modify `V1__init_schema.sql` or `V2__seed_synthetic_data.sql` after they have been applied.
- **Port conflict:** Ensure nothing else is listening on ports 80, 443, or 5432.

### Nginx returns 502 Bad Gateway

The API container is not healthy. Check:

```bash
docker inspect --format='{{.State.Health.Status}}' hospital-api
docker compose logs --tail=30 api
```

Restart the API once the root cause is resolved:

```bash
docker compose restart api
```

### Database is healthy but application cannot connect

Confirm the network settings:

```bash
docker compose config
docker network ls
docker network inspect hospital-management-platform_hospital-net
```

All three containers must be on the `hospital-net` bridge network.

### SSH permission denied on EC2

The PEM key must have restricted permissions. This only works on a native Linux/WSL filesystem — not on NTFS mounts under `/mnt/c/`:

```bash
cp /mnt/c/path/to/hospital-platform-key.pem ~/.ssh/
chmod 400 ~/.ssh/hospital-platform-key.pem
ssh -i ~/.ssh/hospital-platform-key.pem ubuntu@<EC2_IP>
```

### Regenerating TLS certificates

Certificates are stored in `nginx/certs/`. Delete and regenerate:

```bash
rm nginx/certs/server.crt nginx/certs/server.key
bash nginx/generate-certs.sh
docker compose restart nginx
```

On Windows:

```powershell
Remove-Item nginx\certs\server.crt, nginx\certs\server.key
.\nginx\generate-certs.ps1
docker compose restart nginx
```

---

## Useful One-Liners

```bash
# Check the JWT token expiry (decode the payload without verifying)
echo "<token>" | cut -d. -f2 | base64 -d 2>/dev/null | python3 -m json.tool

# Count total appointments in the database
docker exec hospital-postgres psql -U hospital_user -d hospital_db -c "SELECT COUNT(*) FROM appointments;"

# Check audit log for recent events
docker exec hospital-postgres psql -U hospital_user -d hospital_db -c "SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT 10;"

# View current disk usage on EC2
df -h

# View Docker disk usage
docker system df
```
