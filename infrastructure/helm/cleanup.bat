@echo off
REM Complete cleanup script for Helm and Kafka

echo =========================================
echo   Cleanup - Demo App and Infrastructure
echo =========================================
echo.

echo [1/6] Uninstalling Helm releases...
helm uninstall demo --namespace demo --ignore-not-found 2>nul
if errorlevel 1 (
    echo   No Helm release found or already uninstalled
) else (
    echo   Helm release uninstalled
)

echo.
echo [2/6] Deleting cluster-scoped resources...
kubectl delete clusterrole prometheus-demo-app --ignore-not-found=true 2>nul
kubectl delete clusterrolebinding prometheus-demo-app --ignore-not-found=true 2>nul
echo   Done

echo.
echo [3/6] Waiting for resources to terminate...
timeout /t 5 /nobreak >nul
echo   Done

echo.
echo [4/6] Cleaning up Kafka...
echo   Uninstalling Kafka Helm release...
helm uninstall kafka -n demo 2>nul
echo   Waiting for Kafka pods to terminate...
timeout /t 15 /nobreak >nul
kubectl wait --for=delete pod -l app=cp-kafka -n demo --timeout=60s 2>nul
kubectl wait --for=delete pod -l app=cp-zookeeper -n demo --timeout=60s 2>nul
echo   Deleting Kafka PVCs...
kubectl delete pvc -n demo -l app=cp-kafka --ignore-not-found=true 2>nul
kubectl delete pvc -n demo -l app=cp-zookeeper --ignore-not-found=true 2>nul
echo   Kafka cleanup complete

echo.
echo [5/6] Deleting namespace (if empty)...
kubectl delete namespace demo --ignore-not-found=true 2>nul
if errorlevel 1 (
    echo   Namespace already deleted
) else (
    echo   Namespace deleted
)

echo.
echo [6/6] Verifying cleanup...
echo.
echo Checking Helm namespace:
kubectl get namespace demo 2>nul
if errorlevel 1 (
    echo   ✓ Namespace demo: DELETED
) else (
    echo   ⚠ WARNING: Namespace demo still exists
)


echo.
echo =========================================
echo   Cleanup Complete!
echo =========================================
echo.
echo To redeploy everything, run:
echo   deploy.bat deploy
echo.
echo To deploy infrastructure only:
echo   deploy.bat deploy --infra-only
echo.
exit /b 0
