# Strimzi Kafka Quick Start Guide

## ⚠️ Important Note

**This script is called automatically by the main deployment!**

You should normally use:
```cmd
cd infrastructure\helm
deploy.bat deploy
```

This will deploy **both** Strimzi Kafka **and** your application together.

## Overview

Your Kafka infrastructure uses **Strimzi** - a production-ready, widely-adopted Kafka operator for Kubernetes.

## What Changed?

### Before (Custom Charts)
- ❌ 6 pods (3 Zookeeper + 3 Kafka)
- ❌ Manual probe configuration (always failing)
- ❌ Complex Helm templates
- ❌ Manual Zookeeper management

### After (Strimzi)
- ✅ 3 pods (Kafka only, using KRaft)
- ✅ Built-in health checks (works perfectly)
- ✅ Simple declarative CRDs
- ✅ No Zookeeper needed!

## Deployment Methods

### Method 1: Full Deployment (Recommended)

Deploy everything (Kafka + Application):
```cmd
cd infrastructure\helm
deploy.bat deploy
```

### Method 2: Infrastructure Only

Deploy only Kafka and infrastructure:
```cmd
cd infrastructure\helm
deploy.bat deploy --infra-only
```

### Method 3: Standalone Kafka (Advanced)

**Only if you need to deploy Kafka separately:**
```cmd
cd infrastructure\strimzi
deploy.bat
```

**Note:** The main `infrastructure\helm\deploy.bat` calls this automatically!

### Option 2: Manual Commands

```bash
# 1. Add Strimzi Helm repo
helm repo add strimzi https://strimzi.io/charts/
helm repo update

# 2. Create namespaces
kubectl create namespace strimzi-operator
kubectl create namespace spring-boot-demo

# 3. Install Strimzi Operator
helm install strimzi-cluster-operator strimzi/strimzi-kafka-operator \
  --namespace strimzi-operator \
  --values infrastructure/strimzi/operator-values.yaml

# 4. Deploy Kafka Cluster
kubectl apply -f infrastructure/strimzi/kafka-cluster.yaml -n spring-boot-demo

# 5. Wait for Kafka to be ready (2-5 minutes)
kubectl wait --for=condition=ready kafka/demo-kafka-cluster -n spring-boot-demo --timeout=600s
```

## Verify Deployment

```bash
# Check Kafka cluster status
kubectl get kafka -n spring-boot-demo

# Expected output:
# NAME                 DESIRED KAFKA REPLICAS   READY   WARNINGS
# demo-kafka-cluster   3                        True

# Check pods
kubectl get pods -n spring-boot-demo

# Expected output:
# demo-kafka-cluster-kafka-0              1/1     Running
# demo-kafka-cluster-kafka-1              1/1     Running
# demo-kafka-cluster-kafka-2              1/1     Running
# demo-kafka-cluster-entity-operator-...  2/2     Running
```

## Configuration

### Kafka Cluster Specs
- **Brokers**: 3 (KRaft mode)
- **Version**: Kafka 3.9.0
- **Replication Factor**: 3
- **Min In-Sync Replicas**: 2
- **Storage**: 20Gi per broker (60Gi total)
- **Memory**: 1-2Gi per broker
- **Bootstrap Server**: `demo-kafka-cluster-kafka-bootstrap:9092`

### Application Configuration

Your demo app is already configured to use Strimzi:

```yaml
spring:
  kafka:
    bootstrap-servers: demo-kafka-cluster-kafka-bootstrap:9092
```

Location: `infrastructure/helm/templates/demo-app-deployment.yaml:19`

## Test Kafka

### Create Test Topic

```bash
kubectl run kafka-producer -ti --rm=true \
  --image=quay.io/strimzi/kafka:latest-kafka-3.9.0 \
  --restart=Never \
  -n spring-boot-demo -- bin/kafka-topics.sh \
  --bootstrap-server demo-kafka-cluster-kafka-bootstrap:9092 \
  --create --topic test-topic \
  --replication-factor 3 --partitions 3
```

### Produce Message

```bash
kubectl run kafka-producer -ti --rm=true \
  --image=quay.io/strimzi/kafka:latest-kafka-3.9.0 \
  --restart=Never \
  -n spring-boot-demo -- bin/kafka-console-producer.sh \
  --bootstrap-server demo-kafka-cluster-kafka-bootstrap:9092 \
  --topic test-topic

# Type your message and press Enter
```

### Consume Message

```bash
kubectl run kafka-consumer -ti --rm=true \
  --image=quay.io/strimzi/kafka:latest-kafka-3.9.0 \
  --restart=Never \
  -n spring-boot-demo -- bin/kafka-console-consumer.sh \
  --bootstrap-server demo-kafka-cluster-kafka-bootstrap:9092 \
  --topic test-topic \
  --from-beginning
```

## Common Operations

### Scale Brokers

```bash
# Scale to 5 brokers
kubectl patch kafka demo-kafka-cluster -n spring-boot-demo \
  --type merge -p '{"spec":{"kafka":{"replicas":5}}}'

# Scale back to 3
kubectl patch kafka demo-kafka-cluster -n spring-boot-demo \
  --type merge -p '{"spec":{"kafka":{"replicas":3}}}'
```

### Check Logs

```bash
# View broker logs
kubectl logs -n spring-boot-demo demo-kafka-cluster-kafka-0 -f

# View operator logs
kubectl logs -n strimzi-operator -l app.kubernetes.io/name=strimzi-cluster-operator -f
```

### Check Status

```bash
# Detailed status
kubectl describe kafka demo-kafka-cluster -n spring-boot-demo

# Quick status
kubectl get kafka -n spring-boot-demo
```

## Cleanup

### Full Cleanup (Recommended)

Clean up everything (Application + Kafka + Infrastructure):
```cmd
cd infrastructure\helm
cleanup.bat
```

This will:
1. Uninstall Helm releases
2. Delete cluster resources
3. Call Strimzi cleanup (automatically)
4. Delete namespaces

### Standalone Kafka Cleanup (Advanced)

**Only if you deployed Kafka separately:**
```cmd
cd infrastructure\strimzi
cleanup.bat
```

The script will ask you:
- Delete PVCs (Kafka data)?
- Delete namespaces?
- Delete Strimzi CRDs?

**Note:** The main `infrastructure\helm\cleanup.bat` calls this automatically!

## Why Strimzi?

### Production Benefits
1. **Widely Adopted**: Used by thousands of companies
2. **Battle-Tested**: CNCF Sandbox project
3. **No Probe Issues**: Health checks work perfectly
4. **Simpler**: No Zookeeper to manage
5. **Automated**: Operator handles Day-2 operations
6. **Free**: Completely open-source

### Resource Savings
- **50% fewer pods** (3 vs 6)
- **30-40% less memory** (no Zookeeper overhead)
- **33% less storage** (60Gi vs 90Gi)

### Operational Benefits
- **Zero probe configuration** - works out of the box
- **Automated scaling** - simple kubectl patch
- **Rolling upgrades** - operator manages automatically
- **Better observability** - built-in metrics

## Documentation

- **Quick Start**: This file
- **Migration Guide**: `infrastructure/strimzi/MIGRATION_GUIDE.md`
- **Detailed README**: `infrastructure/strimzi/README.md`
- **Official Docs**: https://strimzi.io/docs/operators/latest/deploying

## Support

### Community
- **GitHub**: https://github.com/strimzi/strimzi-kafka-operator
- **Slack**: https://strimzi.io/join-us/
- **Stack Overflow**: Tag `strimzi`

### References
- [Strimzi Documentation](https://strimzi.io/docs/operators/latest/deploying)
- [Strimzi vs Bitnami Comparison](https://www.automq.com/blog/strimzi-vs-bitnami-kafka-kubernetes-comparison)
- [Deploy Kafka with Strimzi on AKS](https://learn.microsoft.com/en-us/azure/aks/kafka-deploy)

## Next Steps

### For Full Deployment

1. ✅ Deploy everything:
   ```cmd
   cd infrastructure\helm
   deploy.bat deploy
   ```
2. ✅ Verify deployment: `kubectl get kafka -n spring-boot-demo`
3. ✅ Check status: `deploy.bat status`
4. ✅ Access your application at http://localhost:31080

### For Infrastructure Only

1. ✅ Deploy infrastructure:
   ```cmd
   cd infrastructure\helm
   deploy.bat deploy --infra-only
   ```
2. ✅ Verify Kafka: `kubectl get kafka -n spring-boot-demo`
3. ✅ Test Kafka: Create topic and produce/consume messages
4. ✅ Deploy app later: `deploy.bat deploy --skip-build`

---

**Congratulations!** You now have a production-ready Kafka cluster with zero configuration headaches! 🎉

All deployment is centralized in `infrastructure\helm\deploy.bat`!
