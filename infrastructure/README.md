# Infrastructure Deployment Guide

## Quick Start (One Command!)

Deploy everything (Kafka + Application + Infrastructure):

```cmd
cd helm
deploy.bat deploy
```

That's it! This single command will:
1. ✅ Deploy Strimzi Kafka (3 brokers, KRaft mode)
2. ✅ Build and deploy your application
3. ✅ Deploy IBM MQ, Prometheus, and Grafana
4. ✅ Wait for everything to be ready

## Architecture

```
infrastructure/
├── helm/                    ⭐ MAIN DEPLOYMENT SCRIPTS
│   ├── deploy.bat          → Deploy everything (calls strimzi/deploy.bat)
│   ├── cleanup.bat         → Cleanup everything (calls strimzi/cleanup.bat)
│   ├── templates/          → Helm templates for app and infrastructure
│   └── values.yaml         → Configuration
│
└── strimzi/                 → Kafka deployment (called automatically)
    ├── deploy.bat          → Deploy Kafka only (called by helm/deploy.bat)
    ├── cleanup.bat         → Cleanup Kafka (called by helm/cleanup.bat)
    ├── kafka-cluster.yaml  → Kafka CRD definition
    └── operator-values.yaml → Strimzi operator config
```

## Deployment Options

### Full Deployment (Recommended)

Deploy everything:
```cmd
cd helm
deploy.bat deploy
```

Includes:
- Strimzi Kafka (3 brokers, KRaft mode)
- Demo Application
- IBM MQ
- Prometheus + Grafana

### Infrastructure Only

Deploy infrastructure without the application:
```cmd
cd helm
deploy.bat deploy --infra-only
```

Includes:
- Strimzi Kafka (3 brokers)
- IBM MQ
- Prometheus + Grafana

**Later, deploy the app:**
```cmd
deploy.bat deploy --skip-build
```

### Skip Docker Build

Use existing Docker image:
```cmd
cd helm
deploy.bat deploy --skip-build
```

## Cleanup

Clean up everything:
```cmd
cd helm
cleanup.bat
```

This will:
1. Uninstall Helm releases
2. Delete cluster resources
3. Cleanup Strimzi Kafka (calls `strimzi/cleanup.bat` automatically)
4. Delete namespaces

## Components

### Strimzi Kafka (Deployed First!)

**What is Strimzi?**
- Production-ready Kafka operator for Kubernetes
- CNCF Sandbox project
- Widely adopted, battle-tested

**Configuration:**
- 3 Kafka brokers (KRaft mode, no Zookeeper!)
- Replication factor: 3
- Min in-sync replicas: 2
- Storage: 20Gi per broker (60Gi total)
- Bootstrap server: `demo-kafka-cluster-kafka-bootstrap:9092`

**Why Strimzi?**
- ✅ Built-in health checks (works perfectly!)
- ✅ No Zookeeper needed (simpler, faster)
- ✅ Automated Day-2 operations
- ✅ Production-proven
- ✅ 50% fewer pods than custom charts
- ✅ 30-40% less memory usage

**Documentation:**
- Quick Start: `strimzi/QUICKSTART.md`
- Detailed Docs: `strimzi/README.md`

### IBM MQ

- 1 instance
- Queue Manager: QM1
- Web Console: https://localhost:31443/ibmmq/console
- Credentials: admin/passw0rd

### Kafka (Strimzi)

- 3 brokers (KRaft mode)
- Bootstrap: demo-kafka-cluster-kafka-bootstrap:9092
- No external access by default (internal only)

### Prometheus

- Monitoring and metrics collection
- URL: http://localhost:31090
- Scrapes: Demo App, IBM MQ, Node Exporter

### Grafana

- Visualization dashboards
- URL: http://localhost:31300
- Credentials: admin/admin
- Pre-configured dashboards for MQ and Kafka

## Access URLs (After Deployment)

- **Demo App**: http://localhost:31080
- **Actuator**: http://localhost:31080/actuator
- **Prometheus**: http://localhost:31090
- **Grafana**: http://localhost:31300 (admin/admin)
- **IBM MQ Console**: https://localhost:31443/ibmmq/console (admin/passw0rd)
- **Kafka**: demo-kafka-cluster-kafka-bootstrap:9092 (internal)

## Status and Troubleshooting

### Check Deployment Status

```cmd
cd helm
deploy.bat status
```

Shows:
- Kafka cluster status
- All pods
- Services
- StatefulSets
- DaemonSets

### Check Kafka Specifically

```bash
kubectl get kafka -n spring-boot-demo
# Should show: READY = True

kubectl get pods -n spring-boot-demo -l strimzi.io/cluster=demo-kafka-cluster
# Should show 3 kafka pods running
```

### View Logs

**Application:**
```bash
kubectl logs -n spring-boot-demo -l app=demo-app -f
```

**Kafka:**
```bash
kubectl logs -n spring-boot-demo demo-kafka-cluster-kafka-0 -f
```

**Strimzi Operator:**
```bash
kubectl logs -n strimzi-operator -l app.kubernetes.io/name=strimzi-cluster-operator -f
```

### Common Issues

**Kafka pods not starting:**
- Check Strimzi operator logs
- Verify PVCs are bound: `kubectl get pvc -n spring-boot-demo`
- Check operator status: `kubectl get pods -n strimzi-operator`

**Application can't connect to Kafka:**
- Verify bootstrap server: `demo-kafka-cluster-kafka-bootstrap:9092`
- Check Kafka is ready: `kubectl get kafka -n spring-boot-demo`
- Check network connectivity

## Advanced Usage

### Standalone Kafka Deployment

**Only needed if deploying Kafka separately:**

```cmd
cd strimzi
deploy.bat
```

**Note:** The main `helm/deploy.bat` calls this automatically!

### Scale Kafka Brokers

```bash
kubectl patch kafka demo-kafka-cluster -n spring-boot-demo \
  --type merge -p '{"spec":{"kafka":{"replicas":5}}}'
```

### Create Kafka Topics

```bash
kubectl run kafka-producer -ti --rm=true \
  --image=quay.io/strimzi/kafka:latest-kafka-3.9.0 \
  --restart=Never \
  -n spring-boot-demo -- bin/kafka-topics.sh \
  --bootstrap-server demo-kafka-cluster-kafka-bootstrap:9092 \
  --create --topic my-topic \
  --replication-factor 3 --partitions 3
```

## Resource Requirements

### Minimum (All Components)

- **CPU**: 4+ cores recommended
- **Memory**: 8GB+ recommended
- **Storage**: 100Gi+ (60Gi for Kafka, 40Gi for other components)

### Per Component

| Component | Pods | Memory | Storage |
|-----------|------|--------|---------|
| **Kafka (Strimzi)** | 3 | 3-6Gi | 60Gi |
| **Entity Operator** | 1 | ~256Mi | - |
| **IBM MQ** | 1 | 512Mi-1Gi | 5Gi (optional) |
| **Demo App** | 1 | 512Mi-1Gi | - |
| **Prometheus** | 1 | 256Mi-512Mi | - |
| **Grafana** | 1 | 256Mi-512Mi | - |
| **Node Exporter** | 1/node | 64Mi-128Mi | - |

## Files Reference

### Main Deployment

- `helm/deploy.bat` - **Main deployment script** ⭐
- `helm/cleanup.bat` - **Main cleanup script** ⭐
- `helm/values.yaml` - Configuration values
- `helm/templates/` - Helm templates

### Strimzi Kafka

- `strimzi/deploy.bat` - Kafka deployment (called by helm/deploy.bat)
- `strimzi/cleanup.bat` - Kafka cleanup (called by helm/cleanup.bat)
- `strimzi/kafka-cluster.yaml` - Kafka cluster definition
- `strimzi/operator-values.yaml` - Operator configuration
- `strimzi/QUICKSTART.md` - Quick start guide
- `strimzi/README.md` - Detailed documentation

### Documentation

- `CLEANUP_SUMMARY.md` - Summary of migration to Strimzi
- This file - Main infrastructure guide

## Summary

**Everything is centralized in `infrastructure/helm/deploy.bat`!**

✅ **One command** deploys everything
✅ **Kafka is deployed first** (Strimzi, production-ready)
✅ **Application depends on Kafka** (deployed after)
✅ **All infrastructure** included (MQ, Prometheus, Grafana)
✅ **Single cleanup** script removes everything

**Start here:**
```cmd
cd infrastructure\helm
deploy.bat deploy
```

That's all you need! 🚀
