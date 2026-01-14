@echo off
REM Windows Batch script to deploy the MQ-Kafka Bridge application to Rancher Desktop
REM Usage: deploy.bat [deploy|undeploy|status] [--skip-build] [--infra-only]

setlocal enabledelayedexpansion

set ACTION=%1
set SKIP_BUILD=0
set INFRA_ONLY=0
set NAMESPACE=demo
set HELM_EXTRA_ARGS=

REM Parse arguments
:parse_args
if "%1"=="" goto done_parsing
if /i "%1"=="--skip-build" set SKIP_BUILD=1
if /i "%1"=="--infra-only" set INFRA_ONLY=1
if /i "%1"=="deploy" set ACTION=deploy
if /i "%1"=="undeploy" set ACTION=undeploy
if /i "%1"=="status" set ACTION=status
shift
goto parse_args
:done_parsing

if "%ACTION%"=="" set ACTION=deploy

REM If infra-only mode, disable demo app and skip build
if %INFRA_ONLY%==1 (
    set SKIP_BUILD=1
    set HELM_EXTRA_ARGS=--set demoApp.enabled=false
)

if /i "%ACTION%"=="deploy" goto deploy
if /i "%ACTION%"=="undeploy" goto undeploy
if /i "%ACTION%"=="status" goto status

echo Invalid action: %ACTION%
echo Usage: deploy.bat [deploy^|undeploy^|status] [--skip-build] [--infra-only]
echo.
echo Options:
echo   --skip-build    Skip Docker image build (use existing image)
echo   --infra-only    Deploy only infrastructure (IBM MQ, Kafka, Prometheus, Grafana)
exit /b 1

REM ========================================
REM Deploy function
REM ========================================
:deploy
if %INFRA_ONLY%==1 (
    echo =========================================
    echo   Deploying Infrastructure Components
    echo =========================================
) else (
    echo =========================================
    echo   Deploying MQ-Kafka Bridge Application
    echo =========================================
)
echo.

REM Switch to rancher-desktop context
echo [1/6] Switching to rancher-desktop context...
kubectl config use-context rancher-desktop >nul 2>&1
if errorlevel 1 (
    echo WARNING: Could not switch to rancher-desktop context. Continuing with current context.
)
echo.

REM Check prerequisites
echo [2/6] Checking prerequisites...
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

REM Deploy Confluent Kafka (first!)
echo [3/6] Deploying Confluent Kafka cluster...
echo   Installing Kafka and ZooKeeper (3 nodes each)...
echo   Using patched local charts (Kubernetes 1.25+ compatible)
echo   This may take 5-10 minutes...

REM Deploy using local patched charts
helm upgrade --install kafka ..\cp-helm-charts ^
    --namespace %NAMESPACE% ^
    --create-namespace ^
    -f kafka-values.yaml ^
    --wait --timeout 15m
if errorlevel 1 (
    echo ERROR: Kafka deployment failed
    echo.
    echo Check logs with:
    echo   kubectl get pods -n %NAMESPACE%
    echo   kubectl logs -n %NAMESPACE% -l app=cp-kafka
    exit /b 1
)
echo   Kafka deployed successfully
echo.

REM Build Docker image
if %SKIP_BUILD%==1 goto skip_build

echo [4/6] Building Docker image...
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

echo   Importing image to Kubernetes (Rancher Desktop)...
docker save demo-app:latest | nerdctl -n k8s.io load >nul 2>&1
if errorlevel 1 (
    echo WARNING: Failed to import image to K8s, trying alternative method...
    docker save demo-app:latest -o demo-app.tar
    nerdctl -n k8s.io load -i demo-app.tar
    del demo-app.tar
)
echo   Image imported to Kubernetes

cd ..\infrastructure\helm
echo.
goto create_namespace

:skip_build
echo [4/6] Skipping Docker image build...
docker images demo-app:latest --format "{{.Repository}}:{{.Tag}}" 2>nul | findstr "demo-app:latest" >nul 2>&1
if errorlevel 1 (
    echo WARNING: demo-app:latest image not found
    echo Press Ctrl+C to cancel or any key to continue...
    pause >nul
) else (
    echo   Using existing demo-app:latest image
    echo   Importing image to Kubernetes (Rancher Desktop)...
    docker save demo-app:latest | nerdctl -n k8s.io load >nul 2>&1
    if errorlevel 1 (
        echo WARNING: Failed to import image to K8s, will try during deployment
    ) else (
        echo   Image imported to Kubernetes
    )
)
echo.

:create_namespace
REM Helm will create namespace if needed
echo [5/6] Preparing deployment...
echo   Namespace: %NAMESPACE%
echo.

REM Install/Upgrade Helm chart
echo [6/6] Deploying Helm chart...
if %INFRA_ONLY%==1 (
    echo   Deploying infrastructure components only ^(demo app disabled^)
)
helm upgrade --install demo . --namespace %NAMESPACE% --create-namespace --wait --timeout 10m %HELM_EXTRA_ARGS%
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
echo [7/7] Waiting for pods to be ready...
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
if %INFRA_ONLY%==0 (
    echo   Demo App:       http://localhost:31080
    echo   Actuator:       http://localhost:31080/actuator
)
echo   Prometheus:     http://localhost:31090
echo   Grafana:        http://localhost:31300 ^(admin/admin^)
echo   IBM MQ Console: https://localhost:31443/ibmmq/console ^(admin/passw0rd^)
echo   Kafka: kafka-cp-kafka:9092
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
echo Kafka Pods:
kubectl get pods -n %NAMESPACE% -l app=cp-kafka 2>nul
if errorlevel 1 (
    echo   Kafka not deployed yet
)
echo.
if %INFRA_ONLY%==1 (
    echo NOTE: Infrastructure-only deployment
    echo To deploy the demo app later, run: deploy.bat deploy --skip-build
    echo   ^(or build and deploy with: deploy.bat deploy^)
    echo.
)
goto end

REM ========================================
REM Undeploy function
REM ========================================
:undeploy
echo Undeploying application...
helm uninstall demo --namespace %NAMESPACE%
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
echo Kafka Pods:
kubectl get pods -n %NAMESPACE% -l app=cp-kafka 2>nul
if errorlevel 1 (
    echo   Kafka not deployed
)
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
