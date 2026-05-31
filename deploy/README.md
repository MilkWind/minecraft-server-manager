# Deployment

This folder contains the Caddy reverse proxy configuration required by `design-complete.md`.

## Caddy responsibilities

- Public entry point for the application.
- Forward `/api/*` requests to the Spring Boot backend.
- Forward all other requests to the Vue frontend instance.
- Terminate HTTPS for the public site address.

## Configuration

The default `Caddyfile` is local-friendly and can be overridden with environment variables:

```powershell
$env:APP_SITE_ADDRESS = "minecraft.example.com"
$env:FRONTEND_UPSTREAM = "127.0.0.1:4173"
$env:BACKEND_UPSTREAM = "127.0.0.1:8080"
caddy run --config deploy/Caddyfile
```

Use a real domain for `APP_SITE_ADDRESS` in production so Caddy can manage HTTPS certificates.

## Validation

Run the validation helper before deployment:

```powershell
.\deploy\validate-caddy.ps1
```

The script validates the Caddyfile syntax with `caddy validate`. Runtime smoke checks should still verify:

- `https://<host>/` loads the frontend.
- `https://<host>/api/public/servers` reaches the backend.
- Manager APIs require authentication through the reverse proxy.
