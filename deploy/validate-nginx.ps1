$ErrorActionPreference = 'Stop'

param(
    [string]$NginxHome = 'D:\server_tools\nginx-1.30.2',
    [string]$ConfigPath = (Join-Path $PSScriptRoot 'nginx.conf')
)

$nginxExe = Join-Path $NginxHome 'nginx.exe'
$nginxPrefix = ($NginxHome -replace '\\', '/').TrimEnd('/') + '/'

if (-not (Test-Path -LiteralPath $nginxExe)) {
    Write-Error "Nginx executable was not found at '$nginxExe'."
}

if (-not (Test-Path -LiteralPath $ConfigPath)) {
    Write-Error "Nginx configuration was not found at '$ConfigPath'."
}

& $nginxExe -t -p $nginxPrefix -c $ConfigPath

if ($LASTEXITCODE -ne 0) {
    throw "Nginx configuration validation failed with exit code $LASTEXITCODE."
}
