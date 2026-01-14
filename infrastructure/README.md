# Infrastructure Deployment Guide

## Quick Start (One Command!)

Deploy everything (Kafka + Application + Infrastructure):

```cmd
cd helm
deploy.bat deploy
```

That's it! This single command will:
1. ✅ Deploy Confluent Kafka (3 brokers, with ZooKeeper)
2. ✅ Build and deploy your application
3. ✅ Deploy IBM MQ, Prometheus, and Grafana
4. ✅ Wait for everything to be ready

## Architecture

```
infrastructure/
├── helm/                    ⭐ MAIN DEPLOYMENT SCRIPTS
│   ├── deploy.bat          → Deploy everything (Kafka + App + Infrastructure)
│   ├── cleanup.bat         → Cleanup everything
│   ├── kafka-values.yaml   → Kafka configuration
│   ├── templates/          → Helm templates for app and infrastructure
│   └── values.yaml         → Configuration
│
└── cp-helm-charts/         → Patched Confluent Kafka charts (Kubernetes 1.25+ compatible)
```

## Deployment Options

### Full Deployment (Recommended)

Deploy everything:
```cmd
cd helm
deploy.bat deploy
```

Includes:
- Confluent Kafka (3 brokers, with ZooKeeper)
- Kafka Drop5 (web interface)
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
- Confluent Kafka (3 brokers)
- Kafka Drop5
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
1. Uninstall Helm releases (demo app + infrastructure)
2. Uninstall Kafka Helm release
3. Wait for pods to terminate
4. Delete all PVCs
5. Delete demo namespace

## Components

### Confluent Kafka (Deployed First!)

**What is it?**
- Production-ready Kafka deployment using cp-helm-charts
- Classic ZooKeeper + Kafka architecture
- Patched for Kubernetes 1.25+ compatibility

**Configuration:**
- 3 ZooKeeper nodes for coordination
- 3 Kafka brokers for high availability
- Replication factor: 3
- Min in-sync replicas: 2
- Storage: 20Gi per broker (60Gi total)
- Bootstrap server: `kafka-cp-kafka:9092`

**Features:**
- ✅ Persistent storage for data durability
- ✅ Auto-create topics enabled
- ✅ 7-day message retention
- ✅ Production-grade replication
- ✅ JMX metrics for Prometheus

**Configuration File:**
- `helm/kafka-values.yaml` - Centralized Kafka settings

### IBM MQ

- 1 instance
- Queue Manager: QM1
- Web Console: https://localhost:31443/ibmmq/console
- Credentials: admin/passw0rd

### Kafka (Confluent)

- 3 brokers (with ZooKeeper)
- Bootstrap: kafka-cp-kafka:9092
- No external access by default (internal only)

### Kafka Drop5

- Web-based Kafka management interface
- URL: http://localhost:31800
- Features:
  - View topics, partitions, and messages
  - Publish and consume messages
  - Monitor consumer groups and lag
  - Manage Kafka cluster configuration

### Prometheus

- Monitoring and metrics collection
- URL: http://localhost:31090
- Scrapes: Demo App, IBM MQ, Node Exporter

### Grafana

- Visualization dashboards
- URL: http://localhost:31300
- Credentials: admin/admin
- Pre-configured dashboards:
  - **Node Exporter - System Metrics**: CPU, memory, disk, network
  - **Spring Boot Application**: JVM memory, HTTP requests, threads
  - **Kafka Cluster Metrics**: Messages, throughput, brokers, partitions
- Auto-configured Prometheus datasource

## Access URLs (After Deployment)

- **Demo App**: http://localhost:31080
- **Actuator**: http://localhost:31080/actuator
- **Kafka Drop5**: http://localhost:31800
- **Prometheus**: http://localhost:31090
- **Grafana**: http://localhost:31300 (admin/admin)
- **IBM MQ Console**: https://localhost:31443/ibmmq/console (admin/passw0rd)
- **Kafka**: kafka-cp-kafka:9092 (internal)

## Local Development

To run the Spring Boot app locally (outside Kubernetes), you need to port-forward services:

```cmd
cd infrastructure
port-forward.bat
```

See [LOCAL_DEVELOPMENT.md](LOCAL_DEVELOPMENT.md) for detailed instructions.

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
kubectl get pods -n demo -l app=cp-kafka
# Should show 3 kafka pods running

kubectl get pods -n demo -l app=cp-zookeeper
# Should show 3 zookeeper pods running
```

### View Logs

**Application:**
```bash
kubectl logs -n demo -l app=demo-app -f
```

**Kafka:**
```bash
kubectl logs -n demo kafka-cp-kafka-0 -c cp-kafka-broker -f
```

**ZooKeeper:**
```bash
kubectl logs -n demo kafka-cp-zookeeper-0 -f
```

### Common Issues

**Kafka pods not starting:**
- Check pod status: `kubectl describe pod kafka-cp-kafka-0 -n demo`
- Verify PVCs are bound: `kubectl get pvc -n demo`
- Check events: `kubectl get events -n demo --sort-by='.lastTimestamp'`

**Application can't connect to Kafka:**
- Verify bootstrap server: `kafka-cp-kafka:9092`
- Check Kafka pods are running: `kubectl get pods -n demo -l app=cp-kafka`
- Test connectivity: `kubectl run test-client --rm -ti --image=confluentinc/cp-kafka:6.1.0 -n demo -- kafka-topics --bootstrap-server kafka-cp-kafka:9092 --list`

## Advanced Usage

### Scale Kafka Brokers

**Note:** Kafka is deployed as part of the main helm/deploy.bat script.

```bash
# Scale Kafka brokers to 5
kubectl scale statefulset kafka-cp-kafka -n demo --replicas=5

# Scale back to 3
kubectl scale statefulset kafka-cp-kafka -n demo --replicas=3
```

### Create Kafka Topics

```bash
kubectl run kafka-client --restart=Never \
  --image=confluentinc/cp-kafka:6.1.0 \
  -n demo -- kafka-topics \
  --bootstrap-server kafka-cp-kafka:9092 \
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
| **Kafka (Confluent)** | 3 | 3-6Gi | 60Gi |
| **ZooKeeper** | 3 | 1.5-3Gi | 60Gi |
| **Kafka Drop5** | 1 | 256Mi-512Mi | - |
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

### Confluent Kafka

- `helm/kafka-values.yaml` - Kafka configuration (ZooKeeper + Kafka)
- `cp-helm-charts/` - Patched Confluent charts (Kubernetes 1.25+ compatible)

### Documentation

- `KAFKA_MIGRATION.md` - Migration guide (Confluent → Confluent cp-helm-charts)
- This file - Main infrastructure guide

## Summary

**Everything is centralized in `infrastructure/helm/deploy.bat`!**

✅ **One command** deploys everything
✅ **Kafka is deployed first** (Confluent, production-ready)
✅ **Kafka Drop5** for easy management and monitoring
✅ **Application depends on Kafka** (deployed after)
✅ **All infrastructure** included (MQ, Prometheus, Grafana)
✅ **Single cleanup** script removes everything

**Start here:**
```cmd
cd infrastructure\helm
deploy.bat deploy
```

That's all you need! 🚀
