# Windows Deployment Guide

This guide describes how to build, upload, configure, and launch Minecraft Server Manager on a Windows host with Nginx as the reverse proxy.

## Target layout

Use this layout on the deployment machine:

```text
D:\minecraft-server-manager\
  backend\
    manager-0.0.1-SNAPSHOT.jar
  frondend\
    dist\
      index.html
      favicon.svg
      icons.svg
      assets\

D:\server_tools\
  nginx-1.30.2\
    nginx.exe
    conf\
      nginx.conf
```

The path `frondend` is intentionally written this way because the current deployment folder uses that name. If you rename it to `frontend`, also update `root` in `deploy/nginx.conf` and `FRONTEND_DIST` in `deploy/start-minecraft-manager.bat`.

## Prerequisites

- Windows host with Java 17 or newer available on `PATH`.
- Nginx extracted to `D:\server_tools\nginx-1.30.2`.
- Backend port `8080` available.
- Public web port `80` available.
- If deploying from another machine, a way to copy files to the Windows host, such as Remote Desktop, SMB share, `scp`, or a zip upload.

Check Java:

```powershell
java -version
```

Check ports:

```powershell
netstat -ano | findstr ":80"
netstat -ano | findstr ":8080"
```

## Build artifacts

Run these commands from the repository root on the build machine.

Build the backend jar:

```powershell
cd backend-server
.\mvnw.cmd clean package
```

The backend artifact is:

```text
backend-server\target\manager-0.0.1-SNAPSHOT.jar
```

Build the frontend static files:

```powershell
cd ..\frontend-client
pnpm install
pnpm build
```

The frontend artifact directory is:

```text
frontend-client\dist
```

## Upload files

Create the deployment folders on the Windows host:

```powershell
New-Item -ItemType Directory -Force D:\minecraft-server-manager\backend
New-Item -ItemType Directory -Force D:\minecraft-server-manager\frondend
```

Copy the backend jar:

```powershell
Copy-Item .\backend-server\target\manager-0.0.1-SNAPSHOT.jar D:\minecraft-server-manager\backend\manager-0.0.1-SNAPSHOT.jar -Force
```

Copy the frontend `dist` directory:

```powershell
Remove-Item D:\minecraft-server-manager\frondend\dist -Recurse -Force -ErrorAction SilentlyContinue
Copy-Item .\frontend-client\dist D:\minecraft-server-manager\frondend\dist -Recurse -Force
```

Copy deployment scripts and Nginx config:

```powershell
Copy-Item .\deploy\nginx.conf D:\server_tools\nginx-1.30.2\conf\nginx.conf -Force
Copy-Item .\deploy\start-minecraft-manager.bat D:\minecraft-server-manager\start-minecraft-manager.bat -Force
```

Optional but useful:

```powershell
Copy-Item .\deploy\validate-nginx.ps1 D:\minecraft-server-manager\validate-nginx.ps1 -Force
```

## Configure backend runtime settings

The packaged jar includes the default `application.yaml`. For production-specific settings, place an external config file next to the jar:

```text
D:\minecraft-server-manager\backend\application.yaml
```

Spring Boot automatically reads config from the working directory when the script starts the jar from `D:\minecraft-server-manager\backend`.

Use this external file for host-specific settings such as:

- Managed Minecraft server root directories.
- JVM arguments for each Minecraft server.
- Public server addresses.
- Manager registration settings.
- Database path, if you do not want the default `.\data\minecraft-manager.db`.

Keep secrets and machine-local paths out of git.

## Validate Nginx

Run:

```powershell
D:\server_tools\nginx-1.30.2\nginx.exe -t -p D:/server_tools/nginx-1.30.2/ -c D:\server_tools\nginx-1.30.2\conf\nginx.conf
```

Expected result:

```text
nginx: the configuration file ... syntax is ok
nginx: configuration file ... test is successful
```

If this fails, do not start the app. Fix the path or config error first.

## Launch

Double-click:

```text
D:\minecraft-server-manager\start-minecraft-manager.bat
```

The script:

- Checks Java is available.
- Checks the backend jar exists.
- Checks the frontend `dist\index.html` exists.
- Validates the Nginx config.
- Starts the backend jar from `D:\minecraft-server-manager\backend`.
- Starts Nginx, or reloads it if already running.

Open:

```text
http://localhost/
```

For another machine on the LAN, use:

```text
http://<windows-host-ip>/
```

## Manual launch commands

Backend:

```powershell
cd D:\minecraft-server-manager\backend
java -jar .\manager-0.0.1-SNAPSHOT.jar
```

Nginx:

```powershell
D:\server_tools\nginx-1.30.2\nginx.exe -p D:/server_tools/nginx-1.30.2/ -c D:\server_tools\nginx-1.30.2\conf\nginx.conf
```

Reload Nginx after config changes:

```powershell
D:\server_tools\nginx-1.30.2\nginx.exe -p D:/server_tools/nginx-1.30.2/ -s reload
```

Stop Nginx:

```powershell
D:\server_tools\nginx-1.30.2\nginx.exe -p D:/server_tools/nginx-1.30.2/ -s stop
```

## Smoke tests

Frontend:

```powershell
Invoke-WebRequest http://localhost/ -UseBasicParsing
```

Public API:

```powershell
Invoke-WebRequest http://localhost/api/public/servers -UseBasicParsing
```

SPA route fallback:

```powershell
Invoke-WebRequest http://localhost/servers/MilkWind/visitor -UseBasicParsing
```

Backend health endpoint:

```powershell
Invoke-WebRequest http://localhost:8080/actuator/health -UseBasicParsing
```

The default Nginx config proxies `/api/` only, so backend health is checked directly on port `8080`.

## Updating an existing deployment

1. Build the new backend jar and frontend `dist`.
2. Stop the backend console window, or stop the Java process from Task Manager.
3. Replace `D:\minecraft-server-manager\backend\manager-0.0.1-SNAPSHOT.jar`.
4. Replace `D:\minecraft-server-manager\frondend\dist`.
5. Copy any changed `nginx.conf` or startup script.
6. Double-click `D:\minecraft-server-manager\start-minecraft-manager.bat`.
7. Run the smoke tests.

If only frontend files changed, replace `frondend\dist` and reload the browser. Nginx does not need a restart unless `nginx.conf` changed.

If only Nginx config changed, validate and reload:

```powershell
D:\server_tools\nginx-1.30.2\nginx.exe -t -p D:/server_tools/nginx-1.30.2/ -c D:\server_tools\nginx-1.30.2\conf\nginx.conf
D:\server_tools\nginx-1.30.2\nginx.exe -p D:/server_tools/nginx-1.30.2/ -s reload
```

## Logs and data

Backend console logs appear in the backend command window started by the batch file.

Nginx logs:

```text
D:\server_tools\nginx-1.30.2\logs\access.log
D:\server_tools\nginx-1.30.2\logs\error.log
```

Default SQLite data path when the backend starts from `D:\minecraft-server-manager\backend`:

```text
D:\minecraft-server-manager\backend\data\minecraft-manager.db
```

Back up the `data` directory before replacing or moving the backend deployment.

## HTTPS

The default deployment serves HTTP on port `80`. To enable HTTPS:

1. Put certificate files on the Windows host, for example under `D:\server_tools\certs`.
2. Edit `D:\server_tools\nginx-1.30.2\conf\nginx.conf`.
3. Enable the commented `listen 443 ssl` server block.
4. Set `server_name`, `ssl_certificate`, and `ssl_certificate_key`.
5. Run `nginx.exe -t`.
6. Reload Nginx.

Do not enable the TLS block until the certificate paths are real, or Nginx will fail to start.

## Troubleshooting

`Java was not found on PATH`:
Install Java 17 or newer and reopen the terminal or desktop session.

`Backend port 8080 is already in use`:
The script skips backend startup. If this is not expected, find the process:

```powershell
netstat -ano | findstr ":8080"
```

`Nginx configuration validation failed`:
Run the validation command manually and read the exact line number. Common causes are wrong `root`, wrong certificate paths, or broken quoting around `-p`.

`404 on deep frontend routes`:
Confirm `location / { try_files $uri $uri/ /index.html; }` exists in `nginx.conf`.

`Frontend loads but API fails`:
Confirm the backend jar is running on `127.0.0.1:8080` and Nginx has `location /api/`.

`Nginx says bind() to 0.0.0.0:80 failed`:
Another process is using port `80`. Stop that process or change the `listen` port in `nginx.conf`.
