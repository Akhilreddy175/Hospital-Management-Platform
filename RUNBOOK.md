# Runbook – Hospital Management Platform

This runbook contains the commands used to operate the Doctor Appointment API. Run Docker Compose commands from the project root on the EC2 host or local machine. On Windows, use PowerShell or run the Linux shell scripts through WSL.

**Deployed host:** `3.109.203.28`  
**Health endpoint:** [https://3.109.203.28/actuator/health](https://3.109.203.28/actuator/health)  
**GHCR package:** [hospital-appointment-service](https://github.com/Akhilreddy175/Hospital-Management-Platform/pkgs/container/hospital-appointment-service)

The deployment uses a self-signed TLS certificate. Use `curl -k` for the checks below; this bypasses certificate trust validation and is intended here only for this demonstration endpoint.

## Start and stop the application

Start the services:

```bash
docker compose up -d
```

Check their status:

```bash
docker compose ps
```

Stop the services while keeping the database volume:

```bash
docker compose down
```

**Warning:** `docker compose down -v` also removes the Compose-managed volumes and can delete the database data. Do not use it unless you intentionally want to remove that data and have confirmed a usable backup.

## Check application health

Check the local deployment:

```bash
curl -k https://localhost/actuator/health
```

Check the EC2 deployment:

```bash
curl -k https://3.109.203.28/actuator/health
```

The response should report `UP` when the application and its required dependencies are healthy. You can also check the dedicated probes:

```bash
curl -k https://3.109.203.28/actuator/health/liveness
curl -k https://3.109.203.28/actuator/health/readiness
```

The HTTP endpoint is configured to redirect to HTTPS:

```bash
curl -I http://3.109.203.28/actuator/health
```

A `301` response indicates the redirect. The self-signed certificate means a normal browser may still display a trust warning on the HTTPS destination.

## View logs

Follow logs for all services:

```bash
docker compose logs -f
```

View one service at a time:

```bash
docker compose logs --tail=100 api
docker compose logs --tail=100 nginx
docker compose logs --tail=100 postgres
```

Use the logs before restarting a service. They often show whether the issue is related to application startup, database connectivity or the reverse proxy.

## Restart a service

Restart only the service that needs attention:

```bash
docker compose restart api
docker compose restart nginx
docker compose restart postgres
```

If the API container is repeatedly exiting, inspect its logs and confirm that the database is healthy and the `.env` values match the Compose configuration before trying again:

```bash
docker compose ps
docker compose logs --tail=100 api
```

## Database backup

The project backup script writes a timestamped PostgreSQL dump under the `backups/` directory on the host. Run it from the project root:

```bash
chmod +x scripts/backup.sh
./scripts/backup.sh
```

Check that a new backup file was created:

```bash
ls -lh backups/
```

Keep backup files in a safe location and do not commit them to the repository. The script's retention behaviour is defined in `scripts/backup.sh`; check that file if you need to confirm or change the retention period.

## Restore a database backup

Restoring replaces data in the target database. Before proceeding, take a fresh backup of the current database and confirm that the file you intend to restore is the correct one.

For a maintenance restore, stop the API to prevent requests from changing data while the restore is running:

```bash
docker compose stop api
```

Run the restore script with the path to the selected backup file:

```bash
chmod +x scripts/restore.sh
./scripts/restore.sh ./backups/<backup-file>.sql
```

After the script reports success, start the API and check its health:

```bash
docker compose start api
curl -k https://3.109.203.28/actuator/health
```

If the restore fails, do not assume the database is usable. Review the script output and PostgreSQL logs before restarting normal traffic.

## Roll back the application image

Use a previously published image tag that you know was working. Avoid treating `latest` as a historical rollback version because it may point to the newest image.

```bash
chmod +x scripts/rollback.sh
./scripts/rollback.sh <previous-image-tag>
```

The script updates the configured image tag, pulls the selected image and recreates the API service. Check the script output, then verify the service and health endpoint:

```bash
docker compose ps
curl -k https://3.109.203.28/actuator/health
```

A container replacement may cause a brief interruption; do not describe this single-instance deployment as zero-downtime.

## Troubleshooting

**API is not starting**

```bash
docker compose ps
docker compose logs --tail=100 api
docker compose logs --tail=100 postgres
```

Check the database status and confirm the database connection settings in `.env` match the values expected by Compose and the application.

**Nginx returns `502 Bad Gateway`**

Check whether the API container is running and inspect its logs:

```bash
docker compose ps
docker compose logs --tail=100 api
docker compose logs --tail=100 nginx
```

Resolve the API or database issue before restarting Nginx unnecessarily.

**Database connection fails**

Check the PostgreSQL logs and the database settings in `.env`:

```bash
docker compose logs --tail=100 postgres
docker compose config
```

Do not paste the output of `docker compose config` into public issue trackers without checking it for resolved secret values.

**SSH access to EC2 fails**

Confirm that you are using the correct EC2 key pair, username and host. On Linux or WSL, restrict the key file permissions before connecting:

```bash
chmod 400 <path-to-key.pem>
ssh -i <path-to-key.pem> ubuntu@3.109.203.28
```

**TLS certificate needs to be regenerated**

The certificate files are under `nginx/certs/`. Back up the existing files before replacing them. Use the certificate-generation script for your operating system, then restart Nginx:

```bash
bash nginx/generate-certs.sh
docker compose restart nginx
```

On Windows PowerShell:

```powershell
.\nginx\generate-certs.ps1
docker compose restart nginx
```

Regenerating a self-signed certificate will not make it publicly trusted. A trusted certificate requires a suitable domain name and a certificate issued by a trusted certificate authority.