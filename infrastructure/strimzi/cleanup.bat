@echo off
REM Strimzi Kafka Cleanup Script for Windows
REM This script removes all Strimzi resources automatically

echo =========================================
echo Strimzi Kafka Cleanup
echo =========================================
echo.
echo Deleting all Strimzi resources...
echo.

REM Step 1: Delete Kafka cluster
echo [1/6] Deleting Kafka cluster...
kubectl delete kafka demo-kafka-cluster -n spring-boot-demo --ignore-not-found=true 2>nul
if %ERRORLEVEL% EQU 0 (
    echo   [OK] Kafka cluster deleted
) else (
    echo   [SKIP] Kafka cluster not found
)

REM Step 2: Wait for Kafka resources to be cleaned up
echo [2/6] Waiting for Kafka resources to terminate...
timeout /t 5 /nobreak >nul
echo   [OK] Done

REM Step 3: Delete PVCs
echo [3/6] Deleting Kafka PVCs...
kubectl delete pvc -n spring-boot-demo -l strimzi.io/name=demo-kafka-cluster-kafka --ignore-not-found=true 2>nul
if %ERRORLEVEL% EQU 0 (
    echo   [OK] Kafka PVCs deleted
) else (
    echo   [SKIP] No PVCs found
)

REM Step 4: Delete Entity Operator
echo [4/6] Cleaning up Entity Operator...
kubectl delete deployment -n spring-boot-demo -l strimzi.io/name=demo-kafka-cluster-entity-operator --ignore-not-found=true 2>nul
kubectl delete configmap -n spring-boot-demo -l strimzi.io/name=demo-kafka-cluster-entity-operator --ignore-not-found=true 2>nul
kubectl delete secret -n spring-boot-demo -l strimzi.io/name=demo-kafka-cluster-entity-operator --ignore-not-found=true 2>nul
echo   [OK] Entity Operator cleaned up

REM Step 5: Delete Strimzi Operator
echo [5/6] Uninstalling Strimzi Operator...
helm uninstall strimzi-cluster-operator -n strimzi-operator 2>nul
if %ERRORLEVEL% EQU 0 (
    echo   [OK] Strimzi Operator uninstalled
) else (
    echo   [SKIP] Strimzi Operator not found
)

REM Step 6: Delete namespaces
echo [6/6] Deleting namespaces...
kubectl delete namespace strimzi-operator --ignore-not-found=true 2>nul
echo   [OK] Strimzi cleanup complete

echo.
echo =========================================
echo Cleanup Complete!
echo =========================================
echo.
echo Removed:
echo   - Kafka cluster (demo-kafka-cluster)
echo   - All Kafka PVCs and data
echo   - Entity Operator
echo   - Strimzi Operator
echo   - Strimzi operator namespace
echo.
echo NOTE: spring-boot-demo namespace preserved for other components
echo.
echo To redeploy, run:
echo   %~dp0deploy.bat
echo.
