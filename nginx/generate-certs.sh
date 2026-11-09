#!/bin/bash
# Generate self-signed TLS certificates for local HTTPS testing
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CERTS_DIR="$SCRIPT_DIR/certs"

mkdir -p "$CERTS_DIR"

if [ -f "$CERTS_DIR/server.crt" ] && [ -f "$CERTS_DIR/server.key" ]; then
    echo "Certificates already exist in $CERTS_DIR"
    exit 0
fi

echo "Generating 2048-bit RSA self-signed SSL certificate..."
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout "$CERTS_DIR/server.key" \
  -out "$CERTS_DIR/server.crt" \
  -subj "/C=IN/ST=Punjab/L=Phagwara/O=ALGIONS/OU=DevOps/CN=localhost"

chmod 600 "$CERTS_DIR/server.key"
chmod 644 "$CERTS_DIR/server.crt"

echo "TLS certificates successfully generated in $CERTS_DIR"
