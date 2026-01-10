# Helm Deployment for Rancher Desktop

This directory contains Helm charts for deploying the MQ-Kafka Bridge application to Kubernetes on Rancher Desktop for Windows.

## Overview

The Helm chart provides a **production-like environment locally** with the following configuration:

- **IBM MQ**: 1 instance (StatefulSet)
- **Apache Kafka**: 3 brokers (StatefulSet with Zookeeper)
- **Zookeeper**: 3 instances (StatefulSet for Kafka coordination)
- **Prometheus**: 1 instance with auto-discovery
- **Prometheus Node Exporter**: DaemonSet (metrics from all nodes)
- **Grafana**: 1 instance with Prometheus datasource
- **Demo Application**: 1 instance

All components are configured with:
- StatefulSets for stateful services (Kafka, Zookeeper, IBM MQ)
- No persistence (for faster development iteration)
- NodePort services for easy local access
- Production-like clustering (3 Kafka brokers, 3 Zookeeper instances)
- Prometheus metrics collection from all pods

## Prerequisites

1. **Rancher Desktop** installed and running on Windows
2. **kubectl** configured to use Rancher Desktop context
3. **Helm 3.x** installed
4. **Docker** (comes with Rancher Desktop)
5. **Maven** for building the application
6. **Java 25** for building the application

## Quick Start

### 1. Deploy Everything

From the `infrastructure/helm` directory:

```batch
deploy.bat deploy
```

This will:
1. Check prerequisites (kubectl, helm, docker)
2. Build the Docker image for the demo application
3. Deploy all components to Rancher Desktop
4. Wait for all pods to be ready (may take 2-3 minutes)
5. Display access URLs

### 2. Check Deployment Status

```batch
deploy.bat status
```

Shows:
- Pod status (all pods)
- Services
- StatefulSets
- DaemonSets
- Persistent Volume Claims

### 3. Undeploy Everything

```batch
deploy.bat undeploy
```

Removes all deployed resources and deletes the namespace.

## Access URLs

After deployment, access the services via NodePort:

| Service | URL | Credentials |
|---------|-----|-------------|
| Demo App | http://localhost:31080 | - |
| Actuator | http://localhost:31080/actuator | - |
| Prometheus | http://localhost:31090 | - |
| Grafana | http://localhost:31300 | admin/admin |
| IBM MQ Console | https://localhost:31443/ibmmq/console | admin/passw0rd |
| Kafka | localhost:31092 | - |

## Architecture

### StatefulSets

The deployment uses StatefulSets for stateful services to provide stable network identities:

- **kafka-0, kafka-1, kafka-2**: Kafka brokers with stable DNS names
- **zookeeper-0, zookeeper-1, zookeeper-2**: Zookeeper ensemble
- **ibmmq-0**: IBM MQ instance

### Kafka Clustering

Kafka runs in clustered mode with 3 brokers:
- **Zookeeper coordination**: Uses Zookeeper for cluster management (not KRaft)
- **Replication factor**: 3 (all messages replicated across brokers)
- **Min in-sync replicas**: 2 (at least 2 replicas must acknowledge writes)
- **Broker IDs**: Automatically assigned based on pod ordinal (0, 1, 2)

### Prometheus Monitoring

Prometheus automatically discovers and scrapes metrics from:
- **Demo Application**: Spring Boot Actuator metrics
- **Node Exporter**: System-level metrics from all nodes
- **Kafka**: Broker metrics (via annotations)
- **Zookeeper**: Coordination metrics
- **IBM MQ**: Queue manager metrics
- **Grafana**: Dashboard metrics
- **Prometheus itself**: Internal metrics

All pods with `prometheus.io/scrape: "true"` annotation are automatically discovered.

## Manual Deployment

If you prefer to deploy manually:

### 1. Build Docker Image

```batch
cd ..\..\demo-app
mvn clean package -DskipTests
docker build -t demo-app:latest .
cd ..\..\infrastructure\helm
```

### 2. Install Helm Chart

```batch
helm install demo-app . --namespace demo-app --create-namespace --wait
```

### 3. Watch Deployment

```batch
kubectl get pods -n demo-app --watch
```

Expected pods:
- `kafka-0`, `kafka-1`, `kafka-2` (Kafka brokers)
- `zookeeper-0`, `zookeeper-1`, `zookeeper-2` (Zookeeper ensemble)
- `ibmmq-0` (IBM MQ)
- `demo-app-*` (Application pod)
- `prometheus-*` (Prometheus)
- `grafana-*` (Grafana)
- `node-exporter-*` (Node exporter DaemonSet, one per node)

## Configuration

Edit `values.yaml` to customize:

### Resource Limits

```yaml
kafka:
  resources:
    requests:
      memory: "512Mi"
      cpu: "250m"
    limits:
      memory: "1Gi"
      cpu: "500m"
```

### Replica Counts

```yaml
kafka:
  replicaCount: 3  # Number of Kafka brokers

zookeeper:
  replicaCount: 3  # Number of Zookeeper instances
```

### Persistence

Persistence is disabled by default for faster development:

```yaml
kafka:
  persistence:
    enabled: false
```

To enable persistence (data survives pod restarts):

```yaml
kafka:
  persistence:
    enabled: true
    size: 10Gi
    storageClass: "local-path"  # Rancher Desktop storage class
```

### NodePort Values

```yaml
demoApp:
  service:
    nodePort: 31080  # Change to another port if needed
```

## Troubleshooting

### Pods Not Starting

Check pod status and events:

```batch
kubectl describe pod <pod-name> -n demo-app
kubectl logs <pod-name> -n demo-app
```

### Check Events

```batch
kubectl get events -n demo-app --sort-by='.lastTimestamp'
```

### Delete and Redeploy

```batch
deploy.bat undeploy
deploy.bat deploy
```

### Kafka Connection Issues

1. Check Kafka brokers are running:
   ```batch
   kubectl get pods -n demo-app -l app=kafka
   ```

2. Check Kafka logs:
   ```batch
   kubectl logs kafka-0 -n demo-app
   kubectl logs kafka-1 -n demo-app
   kubectl logs kafka-2 -n demo-app
   ```

3. Verify Zookeeper ensemble:
   ```batch
   kubectl get pods -n demo-app -l app=zookeeper
   ```

4. Check Zookeeper logs:
   ```batch
   kubectl logs zookeeper-0 -n demo-app
   ```

5. Test Kafka connectivity from a pod:
   ```batch
   kubectl exec -it kafka-0 -n demo-app -- /bin/bash
   kafka-broker-api-versions.sh --bootstrap-server localhost:9092
   ```

### IBM MQ Connection Issues

1. Check MQ pod:
   ```batch
   kubectl get pods -n demo-app -l app=ibmmq
   ```

2. Check MQ logs:
   ```batch
   kubectl logs ibmmq-0 -n demo-app
   ```

3. Access MQ Console:
   - URL: https://localhost:31443/ibmmq/console
   - Username: admin
   - Password: passw0rd
   - Check queue: DEV.QUEUE.1

### Zookeeper Issues

1. Check Zookeeper status:
   ```batch
   kubectl exec -it zookeeper-0 -n demo-app -- zkServer.sh status
   ```

2. Verify Zookeeper ensemble:
   ```batch
   kubectl exec -it zookeeper-0 -n demo-app -- zkCli.sh -server localhost:2181 ls /
   ```

### View All Resources

```batch
kubectl get all -n demo-app
```

## Monitoring

### Prometheus Targets

Check which targets Prometheus is scraping:

1. Open http://localhost:31090
2. Go to Status → Targets
3. Verify all targets are UP:
   - spring-boot-app
   - node-exporter
   - kafka (3 instances)
   - zookeeper (3 instances)
   - ibmmq
   - grafana
   - prometheus

### Grafana Dashboards

1. Open http://localhost:31300
2. Login: admin/admin
3. Add Prometheus datasource:
   - URL: http://prometheus:9090
   - Save & Test
4. Create or import dashboards for:
   - Spring Boot Metrics
   - Kafka Cluster
   - Node Exporter (system metrics)
   - Zookeeper Ensemble

### Example Prometheus Queries

- **Kafka messages**: `kafka_server_brokertopicmetrics_messagesin_total`
- **MQ messages received**: `mq_messages_received_total`
- **Kafka messages sent**: `kafka_messages_sent_total`
- **Application health**: `up{job="spring-boot-app"}`
- **CPU usage**: `node_cpu_seconds_total`
- **Memory usage**: `node_memory_MemAvailable_bytes`

## Scaling

### Scale Application

```batch
kubectl scale deployment demo-app --replicas=3 -n demo-app
```

Or update `values.yaml`:

```yaml
demoApp:
  replicaCount: 3
```

Then upgrade:

```batch
helm upgrade demo-app . --namespace demo-app
```

### Scale Kafka (requires recreation)

Scaling Kafka requires updating the StatefulSet:

```yaml
kafka:
  replicaCount: 5  # Scale to 5 brokers
```

Then upgrade:

```batch
helm upgrade demo-app . --namespace demo-app --wait
```

**Note**: Scaling StatefulSets is supported, but scaling down may require manual intervention to ensure data is properly replicated.

## Uninstalling

### Remove Everything

```batch
deploy.bat undeploy
```

### Manual Uninstall

```batch
helm uninstall demo-app --namespace demo-app
kubectl delete namespace demo-app
```

## Advanced Usage

### Update Specific Component

```batch
helm upgrade demo-app . --namespace demo-app --set demoApp.replicaCount=2
```

### Dry Run

```batch
helm install demo-app . --namespace demo-app --dry-run --debug
```

### Template Rendering

```batch
helm template demo-app . --namespace demo-app
```

### Helm Chart Verification

```batch
helm lint .
```

## Production Considerations

This configuration is optimized for local development on Rancher Desktop. For production deployment:

1. **Enable Persistence**: Set `persistence.enabled: true` for Kafka, Zookeeper, and IBM MQ
2. **Storage Classes**: Use appropriate storage classes for your cloud provider
3. **Security**: Enable NetworkPolicies, SecurityContexts, and use Kubernetes Secrets
4. **Resource Limits**: Adjust based on workload requirements
5. **High Availability**: Consider anti-affinity rules and PodDisruptionBudgets
6. **Monitoring**: Set up alerting rules in Prometheus
7. **Ingress**: Use Ingress controllers instead of NodePort
8. **Secrets Management**: Use external secret managers (Vault, AWS Secrets Manager)

## Directory Structure

```
infrastructure/helm/
├── Chart.yaml              # Helm chart metadata
├── values.yaml             # Configuration values (single file for all environments)
├── deploy.bat              # Windows batch deployment script
├── README.md               # This file
└── templates/              # Kubernetes manifests
    ├── _helpers.tpl        # Template helpers
    ├── namespace.yaml      # Namespace definition
    ├── kafka-statefulset.yaml         # Kafka cluster (3 brokers)
    ├── zookeeper-statefulset.yaml     # Zookeeper ensemble (3 instances)
    ├── ibmmq-statefulset.yaml         # IBM MQ
    ├── demo-app-deployment.yaml       # Application
    ├── prometheus-deployment.yaml     # Prometheus with RBAC
    ├── prometheus-configmap.yaml      # Prometheus configuration
    ├── node-exporter-daemonset.yaml   # Node exporter (all nodes)
    ├── grafana-deployment.yaml        # Grafana
    ├── networkpolicy.yaml             # Network policies (optional)
    └── resourcequota.yaml             # Resource quotas (optional)
```

## Support

For issues or questions:
1. Check the troubleshooting section above
2. Review Kubernetes events: `kubectl get events -n demo-app`
3. Check pod logs: `kubectl logs <pod-name> -n demo-app`
4. Verify Rancher Desktop is running and kubectl context is correct
