@echo off
REM Windows Batch script to deploy the MQ-Kafka Bridge application to Rancher Desktop
REM Usage: deploy.bat [deploy|undeploy|status] [--skip-build]

setlocal enabledelayedexpansion

set ACTION=%1
set SKIP_BUILD=0
set NAMESPACE=demo-app

REM Parse arguments
:parse_args
if "%1"=="" goto done_parsing
if /i "%1"=="--skip-build" set SKIP_BUILD=1
if /i "%1"=="deploy" set ACTION=deploy
if /i "%1"=="undeploy" set ACTION=undeploy
if /i "%1"=="status" set ACTION=status
shift
goto parse_args
:done_parsing

if "%ACTION%"=="" set ACTION=deploy

if /i "%ACTION%"=="deploy" goto deploy
if /i "%ACTION%"=="undeploy" goto undeploy
if /i "%ACTION%"=="status" goto status

echo Invalid action: %ACTION%
echo Usage: deploy.bat [deploy^|undeploy^|status] [--skip-build]
echo.
echo Options:
echo   --skip-build    Skip Docker image build (use existing image)
exit /b 1

REM ========================================
REM Deploy function
REM ========================================
:deploy
echo =========================================
echo   Deploying MQ-Kafka Bridge Application
echo =========================================
echo.

REM Check prerequisites
echo [1/5] Checking prerequisites...
kubectl version --client >nul 2>&1
if errorlevel 1 (
    echo ERROR: kubectl not found. Please install kubectl.
    exit /b 1
)
echo   kubectl found

helm version >nul 2>&1
if errorlevel 1 (
    echo ERROR: helm not found. Please install helm.
    exit /b 1
)
echo   helm found

docker version >nul 2>&1
if errorlevel 1 (
    echo ERROR: docker not found. Please install Docker.
    exit /b 1
)
echo   Docker found
echo.

REM Build Docker image
if %SKIP_BUILD%==1 goto skip_build

echo [2/5] Building Docker image...
cd ..\..
if not exist "mvnw.cmd" (
    echo ERROR: Maven wrapper not found at project root
    cd infrastructure\helm
    exit /b 1
)

echo   Running Maven build...
call mvnw.cmd clean package -DskipTests
if errorlevel 1 (
    echo ERROR: Maven build failed
    cd infrastructure\helm
    exit /b 1
)

echo   Building Docker image...
cd demo-app
docker build -t demo-app:latest .
if errorlevel 1 (
    echo ERROR: Docker build failed
    cd ..\infrastructure\helm
    exit /b 1
)
echo   Docker image built successfully
cd ..\infrastructure\helm
echo.
goto create_namespace

:skip_build
echo [2/5] Skipping Docker image build...
docker images demo-app:latest --format "{{.Repository}}:{{.Tag}}" 2>nul | findstr "demo-app:latest" >nul 2>&1
if errorlevel 1 (
    echo WARNING: demo-app:latest image not found
    echo Press Ctrl+C to cancel or any key to continue...
    pause >nul
) else (
    echo   Using existing demo-app:latest image
)
echo.

:create_namespace
REM Create namespace
echo [3/5] Creating namespace...
kubectl create namespace %NAMESPACE% --dry-run=client -o yaml | kubectl apply -f -
echo   Namespace ready
echo.

REM Install/Upgrade Helm chart
echo [4/5] Deploying Helm chart...
helm upgrade --install demo-app . --namespace %NAMESPACE% --create-namespace --wait --timeout 10m
if errorlevel 1 (
    echo ERROR: Helm deployment failed
    echo.
    echo Troubleshooting:
    echo   kubectl get pods -n %NAMESPACE%
    echo   kubectl get events -n %NAMESPACE% --sort-by='.lastTimestamp'
    exit /b 1
)
echo   Helm chart deployed successfully
echo.

REM Wait for pods
echo [5/5] Waiting for pods to be ready...
echo   This may take 2-3 minutes...
kubectl wait --for=condition=ready pod --all -n %NAMESPACE% --timeout=300s
if errorlevel 1 (
    echo WARNING: Some pods may not be ready yet
    echo Check status with: deploy.bat status
)
echo.

REM Display deployment info
echo =========================================
echo   Deployment Complete!
echo =========================================
echo.
echo Access URLs:
echo   Demo App:       http://localhost:31080
echo   Actuator:       http://localhost:31080/actuator
echo   Prometheus:     http://localhost:31090
echo   Grafana:        http://localhost:31300 (admin/admin)
echo   IBM MQ Console: https://localhost:31443/ibmmq/console (admin/passw0rd)
echo   Kafka:          localhost:31092
echo.
echo Pods:
kubectl get pods -n %NAMESPACE%
echo.
echo Services:
kubectl get services -n %NAMESPACE%
echo.
echo StatefulSets:
kubectl get statefulsets -n %NAMESPACE%
echo.
goto end

REM ========================================
REM Undeploy function
REM ========================================
:undeploy
echo Undeploying application...
helm uninstall demo-app --namespace %NAMESPACE%
echo.
echo Deleting namespace...
kubectl delete namespace %NAMESPACE% --ignore-not-found=true
echo Application undeployed successfully
goto end

REM ========================================
REM Status function
REM ========================================
:status
echo Deployment Status:
echo.
echo Pods:
kubectl get pods -n %NAMESPACE%
echo.
echo Services:
kubectl get services -n %NAMESPACE%
echo.
echo StatefulSets:
kubectl get statefulsets -n %NAMESPACE%
echo.
echo DaemonSets:
kubectl get daemonsets -n %NAMESPACE%
echo.
goto end

:end
exit /b 0
