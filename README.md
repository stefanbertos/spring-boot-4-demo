# MQ to Kafka Bridge Application

A Spring Boot application that bridges IBM MQ and Apache Kafka, consuming messages from MQ queues and forwarding them to Kafka topics with full observability through Prometheus and Grafana.

## Project Structure

This is a multi-module Maven project:

```
demo/
├── pom.xml                 # Parent POM with common dependencies
├── GUIDELINES.md           # Development guidelines and standards
├── README.md               # This file
├── infrastructure/         # Infrastructure configuration
│   └── helm/               # Helm charts for Kubernetes deployment
│       ├── Chart.yaml
│       ├── values.yaml     # Configuration (production-like for local)
│       ├── deploy.bat      # Windows deployment script
│       ├── README.md       # Helm deployment guide
│       └── templates/      # Kubernetes manifests
└── demo-app/               # Main application module
    ├── pom.xml
    ├── Dockerfile
    └── src/
        ├── main/java/
        └── test/java/
```

## Features

- **Message Bridging**: Consumes messages from IBM MQ and forwards to Apache Kafka
- **ISO 8583 Support**: Handles back ISO transaction messages with proper DTO mapping
- **Metrics & Observability**:
  - Micrometer metrics with annotations (@Counted, @Timed)
  - Prometheus integration with auto-discovery
  - Node exporter for system metrics
  - Pre-configured Grafana dashboards
- **Virtual Threads**: Leverages Java 25 virtual threads for improved concurrency
- **100% Test Coverage**: Comprehensive unit and integration tests
- **Production-like Local Environment**:
  - Kafka cluster with 3 brokers
  - Zookeeper ensemble with 3 instances
  - StatefulSets for stable network identities
  - Kubernetes-native deployment

## Technology Stack

- **Java 25** with virtual threads
- **Spring Boot 4.0.1**
- **IBM MQ 9.3.4.1**
- **Apache Kafka 3.6.1** with Zookeeper
- **Micrometer + Prometheus** for metrics
- **Grafana 10.2.3** for visualization
- **Kubernetes** (via Rancher Desktop)
- **Helm 3.x** for deployment
- **JUnit 5 + Mockito** for testing

## Quick Start

### Prerequisites

- **Java 25** (JDK)
- **Maven 3.9+**
- **Rancher Desktop** for Windows (with Kubernetes enabled)
- **Helm 3.x**
- **kubectl** (comes with Rancher Desktop)

### 1. Start Infrastructure

From the project root directory:

```batch
cd infrastructure\helm
deploy.bat deploy
```

This deploys to Rancher Desktop Kubernetes:
- **IBM MQ** (1 instance on ports 31414, 31443)
- **Apache Kafka** (3 brokers on port 31092)
- **Zookeeper** (3 instances for Kafka coordination)
- **Prometheus** (port 31090 with auto-discovery)
- **Prometheus Node Exporter** (DaemonSet for system metrics)
- **Grafana** (port 31300)
- **Demo Application** (port 31080)

The deployment takes 2-3 minutes. Wait for all pods to be ready.

### 2. Verify Deployment

Check pod status:

```batch
deploy.bat status
```

Expected pods:
- `kafka-0`, `kafka-1`, `kafka-2` (Kafka brokers)
- `zookeeper-0`, `zookeeper-1`, `zookeeper-2` (Zookeeper ensemble)
- `ibmmq-0` (IBM MQ)
- `demo-app-*` (Application)
- `prometheus-*` (Prometheus)
- `grafana-*` (Grafana)
- `node-exporter-*` (Node exporter on each node)

### 3. Access Services

- **Application**: http://localhost:31080
- **Actuator**: http://localhost:31080/actuator/health
- **Prometheus**: http://localhost:31090
- **Grafana**: http://localhost:31300 (admin/admin)
- **IBM MQ Console**: https://localhost:31443/ibmmq/console (admin/passw0rd)

### 4. Undeploy

```batch
cd infrastructure\helm
deploy.bat undeploy
```

## Development Workflow

### Local Development (without Kubernetes)

For local development and testing:

1. Start the infrastructure:
   ```batch
   cd infrastructure\helm
   deploy.bat deploy
   ```

2. Build and run the application locally:
   ```batch
   cd demo-app
   mvn spring-boot:run
   ```

3. The application will connect to the Kubernetes-deployed MQ and Kafka

### Deploy to Kubernetes

To deploy the application to Kubernetes:

```batch
cd infrastructure\helm
deploy.bat deploy
```

The script will:
1. Build the Docker image
2. Deploy all components via Helm
3. Wait for pods to be ready
4. Display access URLs

## Testing

### Run All Tests

```bash
mvn clean test
```

### Run Integration Tests Only

```bash
mvn clean verify -Dtest=*IntegrationTest
```

### Run Unit Tests Only

```bash
mvn clean test -Dtest=!*IntegrationTest
```

## Code Quality & Static Analysis

The project includes several static analysis tools to ensure code quality:

### Run All Quality Checks

```bash
mvn clean verify
```

This will run:
- **SpotBugs**: Bug pattern detection
- **Checkstyle**: Code style verification
- **PMD**: Source code analysis and CPD (Copy-Paste Detection)

### Run Individual Tools

```bash
# SpotBugs only
mvn spotbugs:check

# Checkstyle only
mvn checkstyle:check

# PMD only
mvn pmd:check pmd:cpd-check
```

### View Reports

After running `mvn verify`, reports are generated in:
- **SpotBugs**: `demo-app/target/spotbugs.xml`
- **Checkstyle**: `demo-app/target/checkstyle-result.xml`
- **PMD**: `demo-app/target/pmd.xml`

### Configuration Files

- `checkstyle.xml` - Checkstyle rules (120 char line limit)
- `pmd-ruleset.xml` - PMD rules
- `spotbugs-exclude.xml` - SpotBugs exclusions

## Architecture

### Kafka Clustering

The deployment includes a production-like Kafka cluster:
- **3 Kafka brokers** (kafka-0, kafka-1, kafka-2)
- **3 Zookeeper instances** for coordination
- **Replication factor 3**: All messages replicated across all brokers
- **Min in-sync replicas 2**: At least 2 replicas must acknowledge writes
- **Zookeeper-based**: Uses Zookeeper for cluster management (not KRaft mode)

### Monitoring

Prometheus automatically discovers and scrapes metrics from:
- **Application**: Spring Boot Actuator metrics
- **Node Exporter**: System-level metrics (CPU, memory, disk, network)
- **Kafka**: Broker metrics from all 3 brokers
- **Zookeeper**: Ensemble metrics from all 3 instances
- **IBM MQ**: Queue manager metrics
- **Grafana**: Dashboard metrics
- **Prometheus**: Self-monitoring

## Message Format

The application forwards messages from IBM MQ to Apache Kafka **as-is** without any transformation. The converter performs a pass-through operation.

### Example: ISO 8583 Banking Message

```
02001234567890123456000000000000010000010112000012345612000001015411123456123456789012AUTH1200TERM0001MERCHANT000001840Additional data
```

The application includes an ISO 8583 parser utility (`Iso8583Parser`) that can be used if message parsing is needed in the future. The `BackIsoTransactionMessage` DTO is also available for structured message handling.

### Message Flow

1. Message arrives at IBM MQ queue (DEV.QUEUE.1)
2. MessageListener consumes the message
3. Converter passes the message through unchanged
4. KafkaMessageService forwards the message to Kafka topic (mq-messages)
5. Message arrives at Kafka in the exact same format as it was in MQ
6. Kafka replicates the message across all 3 brokers

## Configuration

### Application Configuration

Main configuration is in `demo-app/src/main/resources/application.yaml`:
- IBM MQ connection settings
- Kafka bootstrap servers
- Topic names
- Actuator endpoints

### Kubernetes Configuration

Helm configuration is in `infrastructure/helm/values.yaml`:
- Resource limits for all components
- Replica counts (Kafka: 3, Zookeeper: 3, MQ: 1, App: 1)
- NodePort mappings
- Image tags and repositories
- Persistence settings (disabled by default for local)

## Metrics

Available application metrics:
- `mq.messages.received` - Counter for MQ messages received
- `kafka.messages.sent` - Counter for Kafka messages sent
- `message.processing.time` - Timer for end-to-end processing time

System metrics (via Node Exporter):
- CPU usage, load average
- Memory usage (available, used, buffers, cache)
- Disk I/O, disk space
- Network traffic

Access metrics:
- Application: http://localhost:31080/actuator/metrics
- Prometheus: http://localhost:31090
- Grafana: http://localhost:31300

## Development Guidelines

See [GUIDELINES.md](GUIDELINES.md) for comprehensive development standards including:
- Constructor-based dependency injection
- Virtual threads usage
- Java records for DTOs
- 100% test coverage requirements
- Code quality standards
- Architecture testing with ArchUnit
- Modularity with Spring Modulith

## Troubleshooting

### Application won't connect to MQ

1. Verify MQ pod is running:
   ```batch
   kubectl get pods -n demo-app -l app=ibmmq
   ```

2. Check MQ logs:
   ```batch
   kubectl logs ibmmq-0 -n demo-app
   ```

3. Verify queue exists in MQ console:
   - Open: https://localhost:31443/ibmmq/console
   - Login: admin/passw0rd
   - Check queue: DEV.QUEUE.1

### Messages not appearing in Kafka

1. Check Kafka broker status:
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

4. List Kafka topics:
   ```batch
   kubectl exec -it kafka-0 -n demo-app -- kafka-topics.sh --list --bootstrap-server localhost:9092
   ```

### Metrics not in Prometheus/Grafana

1. Check application metrics endpoint:
   http://localhost:31080/actuator/prometheus

2. Check Prometheus targets:
   - Open: http://localhost:31090
   - Go to Status → Targets
   - Verify all targets are UP

3. Check pod discovery:
   ```batch
   kubectl get pods -n demo-app -o wide
   ```

4. Verify Prometheus RBAC:
   ```batch
   kubectl get clusterrolebinding prometheus-demo-app
   ```

### Pods Not Starting

1. Check events:
   ```batch
   kubectl get events -n demo-app --sort-by='.lastTimestamp'
   ```

2. Describe problematic pod:
   ```batch
   kubectl describe pod <pod-name> -n demo-app
   ```

3. Check Rancher Desktop resources:
   - Ensure sufficient memory (at least 6GB allocated)
   - Ensure sufficient CPU (at least 4 cores)

### Complete Restart

If all else fails, completely undeploy and redeploy:

```batch
cd infrastructure\helm
deploy.bat undeploy
REM Wait 30 seconds
deploy.bat deploy
```

## More Information

- **Helm Deployment Guide**: [infrastructure/helm/README.md](infrastructure/helm/README.md)
- **Development Guidelines**: [GUIDELINES.md](GUIDELINES.md)
- **Kafka Configuration**: [values.yaml](infrastructure/helm/values.yaml)

## License

This project is for demonstration purposes.
