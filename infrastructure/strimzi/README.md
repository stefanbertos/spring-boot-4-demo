# Strimzi Kafka Deployment Guide

## Why Strimzi?

Strimzi is the **most widely adopted, production-ready Kafka solution** for Kubernetes:

- **Free & Open Source**: CNCF Sandbox project, no licenses needed
- **Production Battle-Tested**: Used by thousands of organizations
- **Built-in Health Checks**: Proper probes that actually work
- **Operator-Based**: Automated Day-2 operations
- **KRaft Mode**: No Zookeeper needed (simpler, faster, more reliable)
- **Well-Documented**: Extensive documentation and community support

## Architecture

### KRaft vs Zookeeper

Strimzi 0.46+ uses **KRaft mode** by default:
- ✅ No Zookeeper to manage (simpler architecture)
- ✅ Better performance and faster startup
- ✅ Fewer moving parts = more reliable
- ✅ Modern Kafka 3.x+ architecture

## Installation Steps

### 1. Install Strimzi Operator

```bash
# Add Strimzi Helm repository
helm repo add strimzi https://strimzi.io/charts/
helm repo update

# Create namespace for operator
kubectl create namespace strimzi-operator

# Install operator with 3 replicas for HA
helm install strimzi-cluster-operator strimzi/strimzi-kafka-operator \
  --namespace strimzi-operator \
  --set replicas=3 \
  --set watchNamespaces="{demo}" \
  --set leaderElection.enabled=true \
  --set podDisruptionBudget.enabled=true

# Verify operator is running
kubectl get pods -n strimzi-operator
```

### 2. Deploy Kafka Cluster

```bash
# Create namespace for Kafka
kubectl create namespace demo

# Deploy Kafka cluster
kubectl apply -f infrastructure/strimzi/kafka-cluster.yaml -n demo

# Watch deployment (takes 2-5 minutes)
kubectl get kafka -n demo -w
```

### 3. Verify Deployment

```bash
# Check Kafka cluster status
kubectl get kafka -n demo

# Should show:
# NAME                DESIRED KAFKA REPLICAS   DESIRED ZK REPLICAS   READY   WARNINGS
# kafka-cluster  3                                              True

# Check pods
kubectl get pods -n demo

# Should show:
# kafka-cluster-kafka-0        1/1     Running
# kafka-cluster-kafka-1        1/1     Running
# kafka-cluster-kafka-2        1/1     Running
# kafka-cluster-entity-operator-...  Running
```

### 4. Test Kafka

```bash
# Create test topic
kubectl run kafka-producer -ti --rm=true \
  --image=quay.io/strimzi/kafka:latest-kafka-3.9.0 \
  --restart=Never \
  -n demo -- bin/kafka-topics.sh \
  --bootstrap-server kafka-cluster-kafka-bootstrap:9092 \
  --create --topic test-topic \
  --replication-factor 3 \
  --partitions 3

# Produce message
kubectl run kafka-producer -ti --rm=true \
  --image=quay.io/strimzi/kafka:latest-kafka-3.9.0 \
  --restart=Never \
  -n demo -- bin/kafka-console-producer.sh \
  --bootstrap-server kafka-cluster-kafka-bootstrap:9092 \
  --topic test-topic

# Consume message
kubectl run kafka-consumer -ti --rm=true \
  --image=quay.io/strimzi/kafka:latest-kafka-3.9.0 \
  --restart=Never \
  -n demo -- bin/kafka-console-consumer.sh \
  --bootstrap-server kafka-cluster-kafka-bootstrap:9092 \
  --topic test-topic \
  --from-beginning
```

## Connecting Your Application

Update your application configuration:

```yaml
spring:
  kafka:
    bootstrap-servers: kafka-cluster-kafka-bootstrap.demo.svc.cluster.local:9092
```

Or if using the same namespace:

```yaml
spring:
  kafka:
    bootstrap-servers: kafka-cluster-kafka-bootstrap:9092
```

## Advantages Over Custom Charts

| Feature | Custom Charts | Strimzi |
|---------|--------------|---------|
| **Health Checks** | Manual configuration, often broken | Built-in, production-tested |
| **Probe Configuration** | Trial and error | Works out of the box |
| **Day-2 Operations** | Manual intervention | Automated by operator |
| **Upgrades** | Risky, manual | Rolling upgrades, automated |
| **Scaling** | Edit StatefulSet | Simple: `kubectl scale kafka` |
| **Configuration** | Complex Helm templates | Declarative CRDs |
| **Community Support** | Limited | Extensive, CNCF project |
| **Zookeeper Management** | Manual configuration | Not needed (KRaft mode) |

## Configuration Overview

### Kafka Cluster (kafka-cluster.yaml)
- 3 Kafka brokers (KRaft controllers + brokers combined)
- 20Gi persistent storage per broker
- 1Gi-2Gi memory per broker
- Replication factor: 3
- Min in-sync replicas: 2
- Built-in health checks and probes

### Monitoring
- JMX metrics enabled
- Prometheus-compatible
- Built-in Cruise Control for rebalancing

## Scaling

```bash
# Scale up to 5 brokers
kubectl patch kafka kafka-cluster -n demo --type merge -p '{"spec":{"kafka":{"replicas":5}}}'

# Scale down to 3 brokers
kubectl patch kafka kafka-cluster -n demo --type merge -p '{"spec":{"kafka":{"replicas":3}}}'
```

## Upgrading Kafka

```bash
# Edit the Kafka custom resource
kubectl edit kafka kafka-cluster -n demo

# Change version (operator handles rolling upgrade automatically)
spec:
  kafka:
    version: 3.9.0
```

## Troubleshooting

### Check Operator Logs
```bash
kubectl logs -n strimzi-operator -l app.kubernetes.io/name=strimzi-cluster-operator -f
```

### Check Kafka Status
```bash
kubectl describe kafka kafka-cluster -n demo
```

### Check Broker Logs
```bash
kubectl logs -n demo kafka-cluster-kafka-0 -f
```

## Cleanup

```bash
# Delete Kafka cluster
kubectl delete kafka kafka-cluster -n demo

# Delete operator
helm uninstall strimzi-cluster-operator -n strimzi-operator

# Delete namespaces
kubectl delete namespace demo strimzi-operator
```

## References

- [Strimzi Documentation](https://strimzi.io/docs/operators/latest/deploying)
- [Strimzi Helm Chart](https://github.com/strimzi/strimzi-kafka-operator/tree/main/helm-charts)
- [Deploy Kafka with Strimzi on AKS](https://learn.microsoft.com/en-us/azure/aks/kafka-deploy)
- [Strimzi GitHub Releases](https://github.com/strimzi/strimzi-kafka-operator/releases)
- [Strimzi vs Bitnami Comparison](https://www.automq.com/blog/strimzi-vs-bitnami-kafka-kubernetes-comparison)
