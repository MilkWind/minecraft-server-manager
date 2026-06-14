@echo off
setlocal

set "APP_HOME=D:\minecraft-server-manager"
set "BACKEND_DIR=%APP_HOME%\backend"
set "BACKEND_JAR=%BACKEND_DIR%\manager-0.0.1-SNAPSHOT.jar"
set "FRONTEND_DIST=%APP_HOME%\frondend\dist"
set "NGINX_HOME=D:\server_tools\nginx-1.30.2"
set "NGINX_PREFIX=D:/server_tools/nginx-1.30.2/"
set "NGINX_EXE=%NGINX_HOME%\nginx.exe"
set "NGINX_CONF=%~dp0nginx.conf"

if not exist "%NGINX_CONF%" (
    set "NGINX_CONF=%NGINX_HOME%\conf\nginx.conf"
)

echo Starting Minecraft Server Manager...
echo.

where java >nul 2>nul
if errorlevel 1 (
    echo ERROR: Java was not found on PATH.
    pause
    exit /b 1
)

if not exist "%BACKEND_JAR%" (
    echo ERROR: Backend jar was not found:
    echo   %BACKEND_JAR%
    pause
    exit /b 1
)

if not exist "%FRONTEND_DIST%\index.html" (
    echo ERROR: Frontend dist was not found:
    echo   %FRONTEND_DIST%
    pause
    exit /b 1
)

if not exist "%NGINX_EXE%" (
    echo ERROR: Nginx executable was not found:
    echo   %NGINX_EXE%
    pause
    exit /b 1
)

if not exist "%NGINX_CONF%" (
    echo ERROR: Nginx configuration was not found:
    echo   %NGINX_CONF%
    pause
    exit /b 1
)

echo Validating Nginx configuration...
"%NGINX_EXE%" -t -p "%NGINX_PREFIX%" -c "%NGINX_CONF%"
if errorlevel 1 (
    echo ERROR: Nginx configuration validation failed.
    pause
    exit /b 1
)

netstat -ano | findstr /R /C:":8080 .*LISTENING" >nul
if errorlevel 1 (
    echo Starting backend jar...
    start "Minecraft Manager Backend" /D "%BACKEND_DIR%" java -jar "%BACKEND_JAR%"
) else (
    echo Backend port 8080 is already in use. Skipping backend startup.
)

tasklist /FI "IMAGENAME eq nginx.exe" | find /I "nginx.exe" >nul
if errorlevel 1 (
    echo Starting Nginx...
    start "Minecraft Manager Nginx" /D "%NGINX_HOME%" "%NGINX_EXE%" -p "%NGINX_PREFIX%" -c "%NGINX_CONF%"
) else (
    echo Nginx is already running. Reloading configuration...
    "%NGINX_EXE%" -p "%NGINX_PREFIX%" -c "%NGINX_CONF%" -s reload
    if errorlevel 1 (
        echo ERROR: Nginx reload failed.
        pause
        exit /b 1
    )
)

echo.
echo Minecraft Server Manager startup command completed.
echo Open: http://localhost/
pause
