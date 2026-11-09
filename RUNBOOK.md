# Operations & Disaster Recovery Runbook

**Service:** Hospital Appointment Platform  
**Target:** Local VM / Docker Compose / AWS EC2  
**Owner:** Bandaru Akhil Kumar Reddy (DevOps & Cloud Engineering)  

---

## 1. Routine Health & Status Inspection

### Check All Container Health States
```bash
docker compose ps
```
All containers (`hospital-postgres`, `hospital-api`, `hospital-nginx`) must display `Up (healthy)`.

### Test Live Probes Through Nginx HTTPS
```bash
# Liveness Probe: Verifies process is alive
curl -k -i https://localhost/actuator/health/liveness

# Readiness Probe: Verifies DB connectivity and dependency health
curl -k -i https://localhost/actuator/health/readiness
```

---

## 2. Observability & Log Inspection

Logs are output in **Structured JSON format** for high observability:

### Stream Application Logs
```bash
docker compose logs -f api
```

### Inspect Audit Log Events
To review appointment lifecycle and security audits:
```bash
docker compose logs api | grep "AUDIT_EVENT"
```

### Inspect Nginx Access & SSL Logs
```bash
docker compose logs -f nginx
```

---

## 3. Mandatory Failure & Recovery Drill (For Demo Video)

The assessment requires demonstrating **deliberate fault injection and automated recovery**.

### Drill Scenario 1: Sudden API Container Crash
1. **Inject Fault (Kill container):**
   ```bash
   docker kill hospital-api
   ```
2. **Observe Failure:**
   ```bash
   curl -k -i https://localhost/actuator/health
   # Returns 502 Bad Gateway from Nginx
   ```
3. **Demonstrate Automated / Manual Recovery:**
   Because Docker Compose is configured with `restart: unless-stopped`:
   ```bash
   docker compose up -d
   ```
4. **Verify Recovery:**
   Within 15 seconds, the container restarts, passes Actuator health probes, and Nginx resumes serving requests with 200 OK.

---

### Drill Scenario 2: Database Outage & Resilience
1. **Stop Database Service:**
   ```bash
   docker stop hospital-postgres
   ```
2. **Observe Readiness Probe:**
   ```bash
   curl -k https://localhost/actuator/health/readiness
   # Returns 503 Service Unavailable ("db": {"status": "DOWN"})
   ```
3. **Restore Database:**
   ```bash
   docker start hospital-postgres
   ```
4. **Verify Auto-Reconnection:**
   Spring Boot HikariCP pool automatically reconnects without requiring an application restart. Probe returns 200 OK.

---

## 4. Database Backup & Disaster Recovery Procedures

### Standard Backup Procedure
Execute the automated backup script from the project root:
```bash
./scripts/backup.sh
```
* **Storage Location:** Stored on the host in `backups/hospital_db_backup_<YYYYMMDD_HHMMSS>.sql` (outside the container).
* **Retention Policy:** Automatically purges snapshots older than 7 days.

### Standard Disaster Recovery Procedure
If database corruption occurs or data must be restored to a known state:
```bash
./scripts/restore.sh ./backups/hospital_db_backup_<timestamp>.sql
```
1. Script prompts for confirmation.
2. Pipes SQL dump into `hospital-postgres` container via `psql`.
3. Verifies schema integrity and row counts.

---

## 5. Application Rollback Procedure

When a newly deployed image exhibits a critical defect or regression:

1. Identify the previous stable image tag (e.g. `v1.0.0` or previous Git commit SHA).
2. Execute the zero-downtime rollback script:
   ```bash
   ./scripts/rollback.sh <target_tag_or_sha>
   ```
3. The script will:
   * Update `IMAGE_TAG` in `.env`
   * Pull the target tag from GHCR
   * Recreate only the `api` container (`docker compose up -d --no-deps api`)
   * Verify container health status before concluding.

---

## 6. Incident Response Troubleshooting Table

| Symptom | Probable Cause | Action |
| :--- | :--- | :--- |
| **502 Bad Gateway** | Spring Boot container is booting or crashed | Run `docker compose ps` and `docker compose logs api`. Check if DB is ready. |
| **409 Conflict** | Concurrency conflict or duplicate slot | Expected business rule behavior. Inform client to select another slot. |
| **SSL Handshake Error** | Expired or missing certificates | Check `nginx/certs/server.crt` and `server.key`. Re-run `./nginx/generate-certs.sh`. |
| **Flyway Migration Failed** | Incompatible schema change or lock | Inspect `docker compose logs api`. Run `flyway repair` if needed. |
