@echo off
chcp 65001 > nul
title SmartLogistics - Stop All Services

echo Stopping all services on ports 8080-8084...
echo.

for %%p in (8080 8081 8082 8083 8084) do (
    for /f "tokens=5" %%a in ('netstat -ano 2^>nul ^| findstr ":%%p " ^| findstr "LISTENING"') do (
        echo Killing PID %%a on port %%p...
        taskkill /F /PID %%a > nul 2>&1
    )
)

echo.
echo All services stopped.
pause
