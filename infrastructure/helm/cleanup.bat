@echo off
REM Complete cleanup script for both old and new deployments

echo Cleaning up all resources...
echo.

echo [1/5] Uninstalling Helm releases...
helm uninstall demo --namespace spring-boot-demo --ignore-not-found 2>nul
echo   Done

echo.
echo [2/5] Deleting namespaces...
kubectl delete namespace spring-boot-demo --ignore-not-found=true
echo   Done

echo.
echo [3/5] Deleting cluster-scoped resources...
kubectl delete clusterrole prometheus-demo-app --ignore-not-found=true
kubectl delete clusterrolebinding prometheus-demo-app --ignore-not-found=true
echo   Done

echo.
echo [4/5] Waiting for cleanup to complete...
timeout /t 5 /nobreak >nul
echo   Done

echo.
echo [5/5] Verifying cleanup...
kubectl get namespace spring-boot-demo 2>nul
if errorlevel 1 (
    echo   Namespace spring-boot-demo: DELETED
) else (
    echo   WARNING: Namespace spring-boot-demo still exists
)

echo.
echo Cleanup complete! You can now run: deploy.bat deploy --infra-only
exit /b 0
