# Generate self-signed TLS certificates for local HTTPS testing (Windows PowerShell)
$certsDir = Join-Path $PSScriptRoot "certs"

if (-not (Test-Path $certsDir)) {
    New-Item -ItemType Directory -Path $certsDir -Force | Out-Null
}

$crtPath = Join-Path $certsDir "server.crt"
$keyPath = Join-Path $certsDir "server.key"

if ((Test-Path $crtPath) -and (Test-Path $keyPath)) {
    Write-Host "Certificates already exist in $certsDir" -ForegroundColor Green
    exit 0
}

# Auto-detect OpenSSL location (System PATH, Git for Windows, etc.)
$opensslCmd = (Get-Command openssl -ErrorAction SilentlyContinue).Source

if (-not $opensslCmd) {
    $searchPaths = @(
        "C:\Program Files\Git\usr\bin\openssl.exe",
        "${env:ProgramFiles}\Git\usr\bin\openssl.exe",
        "${env:ProgramFiles(x86)}\Git\usr\bin\openssl.exe",
        "$env:LOCALAPPDATA\Programs\Git\usr\bin\openssl.exe"
    )

    foreach ($path in $searchPaths) {
        if (Test-Path $path) {
            $opensslCmd = $path
            break
        }
    }
}

if (-not $opensslCmd) {
    Write-Host "ERROR: OpenSSL could not be found automatically." -ForegroundColor Red
    Write-Host "Tip: If you have Git installed, openssl is usually at 'C:\Program Files\Git\usr\bin\openssl.exe'"
    Write-Host "Alternatively, run this from Git Bash or WSL Ubuntu: ./nginx/generate-certs.sh"
    exit 1
}

Write-Host "Using OpenSSL at: $opensslCmd" -ForegroundColor Cyan
Write-Host "Generating self-signed certificate..."

& "$opensslCmd" req -x509 -nodes -days 365 -newkey rsa:2048 `
  -keyout "$keyPath" `
  -out "$crtPath" `
  -subj "/C=IN/ST=Punjab/L=Phagwara/O=ALGIONS/OU=DevOps/CN=localhost"

if ((Test-Path $crtPath) -and (Test-Path $keyPath)) {
    Write-Host "SUCCESS: TLS certificates successfully generated in $certsDir" -ForegroundColor Green
} else {
    Write-Host "ERROR: Certificate generation failed." -ForegroundColor Red
    exit 1
}
