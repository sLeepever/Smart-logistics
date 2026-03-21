@echo off
chcp 65001 > nul
title SmartLogistics - Backend Launcher

echo ================================================
echo   SmartLogistics Backend Launcher
echo ================================================
echo.

REM ---- Start each service in its own window ----
echo Starting services...
echo.

start "Gateway        :8080" cmd /k "title Gateway :8080 && cd /d %~dp0gateway && mvn spring-boot:run -q 2>&1"
timeout /t 8 /nobreak > nul

start "User-Service   :8081" cmd /k "title User-Service :8081 && cd /d %~dp0user-service && mvn spring-boot:run -q 2>&1"
timeout /t 5 /nobreak > nul

start "Order-Service  :8082" cmd /k "title Order-Service :8082 && cd /d %~dp0order-service && mvn spring-boot:run -q 2>&1"
timeout /t 5 /nobreak > nul

start "Dispatch-Svc   :8083" cmd /k "title Dispatch-Service :8083 && cd /d %~dp0dispatch-service && mvn spring-boot:run -q 2>&1"
timeout /t 5 /nobreak > nul

start "Tracking-Svc   :8084" cmd /k "title Tracking-Service :8084 && cd /d %~dp0tracking-service && mvn spring-boot:run -q 2>&1"

echo.
echo All service windows are opening...
echo Wait ~30 seconds for all services to register with Nacos.
echo.
echo  Gateway        : http://localhost:8080
echo  User-Service   : http://localhost:8081
echo  Order-Service  : http://localhost:8082
echo  Dispatch-Svc   : http://localhost:8083
echo  Tracking-Svc   : http://localhost:8084
echo  Nacos Console  : http://localhost:8848/nacos
echo  Frontend       : http://localhost:5173
echo.
pause
