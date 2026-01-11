@echo off
REM MQ Performance Test Runner
REM Usage: run-test.bat [MESSAGE_COUNT] [RUN_ID]
REM Example: run-test.bat 50000 baseline-v1

setlocal enabledelayedexpansion

REM Parse arguments
set MESSAGE_COUNT=%1
set RUN_ID=%2

REM Set defaults if not provided
if "%MESSAGE_COUNT%"=="" set MESSAGE_COUNT=10000
if "%RUN_ID%"=="" (
    REM Generate unique run ID with timestamp
    for /f "tokens=2 delims==" %%I in ('wmic os get localdatetime /value') do set datetime=%%I
    set RUN_ID=test-!datetime:~0,8!-!datetime:~8,6!
)

echo ================================================================================
echo MQ Performance Test Runner
echo ================================================================================
echo Configuration:
echo   Message Count: %MESSAGE_COUNT%
echo   Run ID: %RUN_ID%
echo ================================================================================
echo.

REM Navigate to helm directory
cd /d "%~dp0..\helm"

echo Deploying performance test job...
helm upgrade mq-perf-test . ^
  --install ^
  --namespace demo ^
  --create-namespace ^
  --set performanceTest.enabled=true ^
  --set performanceTest.messageCount=%MESSAGE_COUNT% ^
  --set performanceTest.runId=%RUN_ID% ^
  --wait ^
  --timeout 10m

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Failed to deploy performance test job
    exit /b 1
)

echo.
echo ================================================================================
echo Performance test job created successfully!
echo ================================================================================
echo.
echo Monitor test execution:
echo   kubectl logs -n demo -l app=mq-performance-test,test-run-id=%RUN_ID% -f
echo.
echo View job status:
echo   kubectl get jobs -n demo -l test-run-id=%RUN_ID%
echo.
echo Port-forward metrics:
echo   kubectl port-forward -n demo service/mq-performance-test-%RUN_ID% 8080:8080
echo.
echo View metrics:
echo   http://localhost:8080/actuator/prometheus
echo.
echo Grafana dashboard:
echo   http://localhost:31300 (admin/admin)
echo.
echo ================================================================================

endlocal
