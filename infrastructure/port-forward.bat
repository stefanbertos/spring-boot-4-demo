@echo off
REM Port-forward script for local development
REM This script forwards Kafka and IBM MQ to standard ports on localhost

echo =========================================
echo   Port Forwarding for Local Development
echo =========================================
echo.

REM Check if kubectl is available
kubectl version --client >nul 2>&1
if errorlevel 1 (
    echo ERROR: kubectl not found
    exit /b 1
)

echo Starting port forwards...
echo.
echo Services will be available at:
echo   Kafka:          localhost:9092
echo   IBM MQ:         localhost:1414
echo.
echo Already accessible via NodePort:
echo   Prometheus:     http://localhost:31090
echo   Grafana:        http://localhost:31300
echo   Kafka Drop5:    http://localhost:31800
echo   IBM MQ Console: https://localhost:31443
echo.
echo Press Ctrl+C to stop all port forwards
echo.

REM Start port forwards in background using PowerShell
echo Starting Kafka port forward (9092)...
start "Kafka Port Forward" /MIN powershell -Command "kubectl port-forward -n demo svc/kafka-cluster-kafka-bootstrap 9092:9092"
timeout /t 2 /nobreak >nul

echo Starting IBM MQ port forward (1414)...
start "IBM MQ Port Forward" /MIN powershell -Command "kubectl port-forward -n demo svc/ibmmq 1414:1414"
timeout /t 2 /nobreak >nul

echo.
echo =========================================
echo   Port Forwards Running!
echo =========================================
echo.
echo Your Spring Boot app can now connect to:
echo   - Kafka at localhost:9092
echo   - IBM MQ at localhost:1414
echo.
echo To stop port forwards:
echo   - Close the minimized PowerShell windows, OR
echo   - Run: stop-port-forward.bat
echo.
echo Press any key to exit this window (port forwards will continue)...
pause >nul
