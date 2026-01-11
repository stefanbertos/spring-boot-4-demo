@echo off
REM Stop all kubectl port-forward processes

echo Stopping all port forwards...

REM Kill all kubectl port-forward processes
taskkill /FI "WINDOWTITLE eq Kafka Port Forward*" /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq IBM MQ Port Forward*" /F >nul 2>&1

REM Also kill any kubectl port-forward processes
for /f "tokens=2" %%a in ('tasklist /FI "IMAGENAME eq kubectl.exe" /FO LIST ^| findstr "PID:"') do (
    taskkill /PID %%a /F >nul 2>&1
)

echo.
echo All port forwards stopped.
echo.
pause
