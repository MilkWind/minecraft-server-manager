# Deployment

This folder contains the Nginx reverse proxy configuration required by `design-complete.md`.

## Nginx responsibilities

- Public entry point for the application.
- Serve the built Vue frontend from `D:\minecraft-server-manager\frondend\dist`.
- Forward `/api/*` requests to the Spring Boot backend on `127.0.0.1:8080`.
- Support Vue Router history fallback through `try_files`.
- Terminate HTTPS for the public site address when TLS certificates are configured.

## Windows layout

The default configuration matches this deployment layout:

```text
D:\minecraft-server-manager\
  backend\manager-0.0.1-SNAPSHOT.jar
  frondend\dist\index.html

D:\server_tools\
  nginx-1.30.2\nginx.exe
  nginx-1.30.2\conf\nginx.conf
```

The folder name `frondend` is intentionally used because it matches the current Windows deployment path. If the deployment folder is renamed to `frontend`, update the `root` directive in `deploy/nginx.conf`.

## Configuration

Copy the repository config into the Nginx install:

```powershell
Copy-Item .\deploy\nginx.conf D:\server_tools\nginx-1.30.2\conf\nginx.conf -Force
```

Or run Nginx with the repository config directly:

```powershell
D:\server_tools\nginx-1.30.2\nginx.exe -p D:\server_tools\nginx-1.30.2\ -c D:\development-projects\personal-projects\minecraft-server-manager\deploy\nginx.conf
```

Start the backend before Nginx:

```powershell
java -jar D:\minecraft-server-manager\backend\manager-0.0.1-SNAPSHOT.jar
```

Start, reload, and stop Nginx:

```powershell
D:\server_tools\nginx-1.30.2\nginx.exe -p D:\server_tools\nginx-1.30.2\ -c D:\server_tools\nginx-1.30.2\conf\nginx.conf
D:\server_tools\nginx-1.30.2\nginx.exe -p D:\server_tools\nginx-1.30.2\ -s reload
D:\server_tools\nginx-1.30.2\nginx.exe -p D:\server_tools\nginx-1.30.2\ -s stop
```

Use the commented TLS server block in `deploy/nginx.conf` when a real domain and certificate files are available.

## Validation

Run the validation helper before deployment:

```powershell
.\deploy\validate-nginx.ps1
```

The script validates `deploy/nginx.conf` with `nginx.exe -t`. Runtime smoke checks should still verify:

- `http://<host>/` loads the frontend.
- `http://<host>/api/public/servers` reaches the backend.
- Deep Vue routes such as `http://<host>/servers/MilkWind/visitor` load through the SPA fallback.
- Manager APIs require authentication through the reverse proxy.
- If TLS is enabled, `https://<host>/` and `https://<host>/api/public/servers` also work.
