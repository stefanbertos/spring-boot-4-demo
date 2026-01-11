@echo off
REM Strimzi Kafka Deployment Script for Windows
REM This script deploys Strimzi Kafka Operator and a 3-broker Kafka cluster

echo =========================================
echo Strimzi Kafka Deployment
echo =========================================
echo.

REM Check if kubectl is installed
where kubectl >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] kubectl is not installed
    exit /b 1
)

REM Check if helm is installed
where helm >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] helm is not installed
    exit /b 1
)

echo [OK] Prerequisites check passed
echo.

REM Step 1: Add Strimzi Helm repository
echo Step 1: Adding Strimzi Helm repository...
helm repo add strimzi https://strimzi.io/charts/ 2>nul
helm repo update >nul 2>nul
echo [OK] Helm repository updated
echo.

REM Step 2: Create namespaces
echo Step 2: Creating namespaces...
kubectl create namespace strimzi-operator 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [WARN] Namespace strimzi-operator already exists
)
kubectl create namespace spring-boot-demo 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [WARN] Namespace spring-boot-demo already exists
)
echo [OK] Namespaces ready
echo.

REM Step 3: Install Strimzi Operator
echo Step 3: Installing Strimzi Operator...
helm upgrade --install strimzi-cluster-operator strimzi/strimzi-kafka-operator --namespace strimzi-operator --values %~dp0operator-values.yaml --wait --timeout 10m
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to install Strimzi Operator
    exit /b 1
)
echo [OK] Strimzi Operator installed
echo.

REM Step 4: Wait for operator to be ready
echo Step 4: Waiting for Strimzi Operator to be ready...
echo    This may take 1-2 minutes...
timeout /t 10 /nobreak >nul
kubectl wait --for=condition=ready pod -l name=strimzi-cluster-operator -n strimzi-operator --timeout=600s 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [WARN] Kubectl wait failed, checking pod status manually...
    kubectl get pods -n strimzi-operator
    echo.
    echo Waiting 30 more seconds...
    timeout /t 30 /nobreak >nul
    kubectl get pods -n strimzi-operator -o wide
    echo.
    REM Check if at least one pod is running
    kubectl get pods -n strimzi-operator -l name=strimzi-cluster-operator --no-headers 2>nul | findstr "Running" >nul
    if %ERRORLEVEL% NEQ 0 (
        echo [ERROR] Strimzi Operator pod is not running
        kubectl describe pod -n strimzi-operator -l name=strimzi-cluster-operator
        exit /b 1
    )
    echo [OK] Strimzi Operator pod is running
) else (
    echo [OK] Strimzi Operator is ready
)
echo.

REM Step 5: Deploy Kafka cluster
echo Step 5: Deploying Kafka cluster (3 brokers with KRaft mode)...
kubectl apply -f %~dp0kafka-cluster.yaml -n spring-boot-demo
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to deploy Kafka cluster
    exit /b 1
)
echo [OK] Kafka cluster deployment initiated
echo.

REM Step 6: Wait for Kafka cluster to be ready
echo Step 6: Waiting for Kafka cluster to be ready (this may take 5-15 minutes)...
echo    You can watch progress with: kubectl get kafka -n spring-boot-demo -w
echo.

REM Wait for Kafka custom resource to be created
timeout /t 10 /nobreak >nul

REM Monitor Kafka cluster status
set max_wait=900
set elapsed=0
set interval=15

:wait_loop
if %elapsed% GEQ %max_wait% goto wait_timeout

REM Get Kafka status
for /f "delims=" %%i in ('kubectl get kafka demo-kafka-cluster -n spring-boot-demo -o jsonpath^="{.status.conditions[?(@.type==\"Ready\")].status}" 2^>nul') do set status=%%i

if "%status%"=="True" (
    echo [OK] Kafka cluster is ready!
    goto wait_done
)

REM Show progress every minute
set /a show_progress=%elapsed% %% 60
if %show_progress%==0 if %elapsed% GTR 0 (
    echo    Progress check:
    kubectl get pods -n spring-boot-demo -l strimzi.io/cluster=demo-kafka-cluster --no-headers 2>nul
)

echo    Waiting... (%elapsed%/%max_wait% seconds) - Status: %status%
timeout /t %interval% /nobreak >nul
set /a elapsed=%elapsed%+%interval%
goto wait_loop

:wait_timeout
echo [ERROR] Kafka cluster did not become ready within %max_wait% seconds
echo.
echo Check status with:
echo   kubectl describe kafka demo-kafka-cluster -n spring-boot-demo
echo   kubectl get pods -n spring-boot-demo
exit /b 1

:wait_done
echo.
echo =========================================
echo Deployment Complete!
echo =========================================
echo.

REM Display cluster information
echo Cluster Information:
echo -------------------
echo Kafka Cluster Name: demo-kafka-cluster
echo Namespace: spring-boot-demo
echo Brokers: 3
echo Mode: KRaft (no Zookeeper)
echo.

echo Bootstrap Server:
echo   Internal: demo-kafka-cluster-kafka-bootstrap:9092
echo   FQDN: demo-kafka-cluster-kafka-bootstrap.spring-boot-demo.svc.cluster.local:9092
echo.

echo Useful Commands:
echo -------------------
echo   # Check Kafka cluster status
echo   kubectl get kafka -n spring-boot-demo
echo.
echo   # Check Kafka pods
echo   kubectl get pods -n spring-boot-demo
echo.
echo   # Check Kafka logs
echo   kubectl logs -n spring-boot-demo demo-kafka-cluster-kafka-0 -f
echo.
echo   # Describe Kafka cluster
echo   kubectl describe kafka demo-kafka-cluster -n spring-boot-demo
echo.

echo [OK] Deployment successful!
