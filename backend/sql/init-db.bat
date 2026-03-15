@echo off
setlocal
echo ========================================
echo  smartLogistics DB Init
echo ========================================
echo.
set PG_USER=postgres
set PG_HOST=localhost
set PG_PORT=5432
set PSQL="D:\DownLoad\pgsql\bin\psql.exe"
set /p PG_PASSWORD=Enter postgres password:
set PGPASSWORD=%PG_PASSWORD%
cd /d "%~dp0"

echo.
echo [1/3] Creating databases...
%PSQL% -U %PG_USER% -h %PG_HOST% -p %PG_PORT% -d postgres -f 00-create-databases.sql
if %errorlevel% neq 0 goto :error

echo.
echo [2/3] Creating tables...
%PSQL% -U %PG_USER% -h %PG_HOST% -p %PG_PORT% -d user_db     -f 01-user_db.sql
if %errorlevel% neq 0 goto :error
%PSQL% -U %PG_USER% -h %PG_HOST% -p %PG_PORT% -d order_db    -f 02-order_db.sql
if %errorlevel% neq 0 goto :error
%PSQL% -U %PG_USER% -h %PG_HOST% -p %PG_PORT% -d dispatch_db -f 03-dispatch_db.sql
if %errorlevel% neq 0 goto :error
%PSQL% -U %PG_USER% -h %PG_HOST% -p %PG_PORT% -d tracking_db -f 04-tracking_db.sql
if %errorlevel% neq 0 goto :error

echo.
echo [3/3] Inserting demo data...
%PSQL% -U %PG_USER% -h %PG_HOST% -p %PG_PORT% -d order_db    -f demo-order_db.sql
if %errorlevel% neq 0 goto :error
%PSQL% -U %PG_USER% -h %PG_HOST% -p %PG_PORT% -d dispatch_db -f demo-dispatch_db.sql
if %errorlevel% neq 0 goto :error
%PSQL% -U %PG_USER% -h %PG_HOST% -p %PG_PORT% -d tracking_db -f demo-tracking_db.sql
if %errorlevel% neq 0 goto :error

echo.
echo ======================================
echo  Done! Start user-service to seed users
echo  Demo password: Demo@1234
echo ======================================
goto :end

:error
echo [ERROR] Script failed, check output above.

:end
set PGPASSWORD=
pause
