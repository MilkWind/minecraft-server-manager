$ErrorActionPreference = 'Stop'

$configPath = Join-Path $PSScriptRoot 'Caddyfile'

if (-not (Get-Command caddy -ErrorAction SilentlyContinue)) {
    Write-Error 'Caddy is not installed or is not available on PATH.'
}

caddy validate --config $configPath --adapter caddyfile
