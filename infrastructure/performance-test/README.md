# MQ Performance Test

Spring Boot application for performance testing IBM MQ message throughput and latency.

## Overview

This application sends a configurable number of messages to IBM MQ and exposes detailed performance metrics via Prometheus. It runs as a Kubernetes Job for on-demand performance testing.

## Features

- Configurable message count and size
- Automatic Prometheus metrics collection
- Integration with existing monitoring stack (Prometheus + Grafana)
- Runs as Kubernetes Job with automatic cleanup
- Spring Boot with Java 25 and virtual threads
- Metrics via annotations (@Counted, @Timed)

## Metrics Exposed

The application exposes the following metrics at `/actuator/prometheus`:

| Metric Name | Type | Description |
|-------------|------|-------------|
| `perf_test_messages_sent_total` | Counter | Total messages sent to MQ |
| `perf_test_message_send_time_seconds` | Timer/Histogram | Time to send each message (p50, p95, p99) |
| `perf_test_duration_seconds` | Gauge | Total test duration |
| `perf_test_throughput` | Gauge | Messages per second |
| `perf_test_status` | Gauge | Test status (1=completed) |

All metrics include labels:
- `test_run_id`: Unique identifier for the test run
- `queue`: MQ queue name

## Building

### Build JAR

```bash
cd C:\Users\stefa\IdeaProjects\demo
mvnw.cmd clean package -pl infrastructure/performance-test
```

### Build Docker Image

```bash
cd infrastructure\performance-test
docker build -t mq-performance-test:latest .
```

Or use the full project build:

```bash
mvnw.cmd clean package
cd infrastructure\performance-test
docker build -t mq-performance-test:latest .
```

## Running

### Quick Start

Use the helper script to run a test with default settings (10,000 messages):

```batch
cd infrastructure\performance-test
run-test.bat
```

### Custom Configuration

```batch
REM Send 50,000 messages with custom run ID
run-test.bat 50000 baseline-v1

REM Send 100,000 messages with auto-generated run ID
run-test.bat 100000
```

### Manual Helm Deployment

```bash
cd infrastructure\helm

helm upgrade mq-perf-test . \
  --install \
  --namespace demo \
  --set performanceTest.enabled=true \
  --set performanceTest.messageCount=20000 \
  --set performanceTest.runId=custom-test-1
```

## Configuration

Edit `infrastructure/helm/values.yaml`:

```yaml
performanceTest:
  enabled: true  # Enable performance testing
  runId: "manual"  # Unique test identifier
  messageCount: 10000  # Number of messages
  messageSize: 1024  # Message size in bytes
  keepAliveMinutes: 5  # Keep app running after test
  ttlAfterFinished: 3600  # Job cleanup time (1 hour)
```

### Environment Variables

Override configuration via environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `MESSAGE_COUNT` | 10000 | Number of messages to send |
| `MESSAGE_SIZE` | 1024 | Size of each message in bytes |
| `TEST_RUN_ID` | manual | Unique identifier for test run |
| `KEEP_ALIVE_MINUTES` | 5 | Time to keep app running for metrics |

## Monitoring

### View Logs

```bash
kubectl logs -n demo -l app=mq-performance-test,test-run-id=manual -f
```

### View Metrics

Port-forward the service:

```bash
kubectl port-forward -n demo service/mq-performance-test-manual 8080:8080
```

Access metrics:
- Prometheus format: http://localhost:8080/actuator/prometheus
- Health check: http://localhost:8080/actuator/health

### Grafana Dashboard

1. Open Grafana: http://localhost:31300 (admin/admin)
2. Navigate to "MQ Performance Test Dashboard"
3. Select your test run from the `test_run_id` dropdown

### Prometheus Queries

Example queries to run in Prometheus (http://localhost:31090):

**Messages sent rate:**
```promql
rate(perf_test_messages_sent_total[1m])
```

**Average latency:**
```promql
rate(perf_test_message_send_time_seconds_sum[1m]) /
rate(perf_test_message_send_time_seconds_count[1m])
```

**p99 latency:**
```promql
histogram_quantile(0.99, rate(perf_test_message_send_time_seconds_bucket[1m]))
```

**Throughput:**
```promql
perf_test_throughput
```

## Troubleshooting

### Job Fails to Start

**Check the job status:**
```bash
kubectl get jobs -n demo -l app=mq-performance-test
kubectl describe job -n demo mq-performance-test-manual
```

**Common issues:**
- IBM MQ not running: `kubectl get pods -n demo -l app=ibmmq`
- Image not available: Check `imagePullPolicy` in values.yaml
- Resource limits: Check cluster has enough CPU/memory

### No Metrics in Prometheus

1. **Check Prometheus targets:**
   - Open http://localhost:31090/targets
   - Find `mq-performance-test` job
   - Should show status UP

2. **Verify pod annotations:**
```bash
kubectl get pod -n demo -l app=mq-performance-test -o yaml | grep -A 5 annotations
```

Should show:
```yaml
annotations:
  prometheus.io/scrape: "true"
  prometheus.io/port: "8080"
  prometheus.io/path: "/actuator/prometheus"
```

3. **Check metrics endpoint:**
```bash
kubectl port-forward -n demo service/mq-performance-test-manual 8080:8080
curl http://localhost:8080/actuator/prometheus | grep perf_test
```

### Job Doesn't Complete

**Check pod logs:**
```bash
kubectl logs -n demo -l app=mq-performance-test,test-run-id=manual
```

**Common issues:**
- MQ connection failure: Check credentials in values.yaml
- Queue doesn't exist: Verify queue name matches MQ configuration
- Out of memory: Increase resource limits in values.yaml

## Performance Tuning

### Increase Message Count

For high-volume tests:

```yaml
performanceTest:
  messageCount: 100000  # 100k messages
  resources:
    requests:
      memory: "512Mi"
      cpu: "500m"
    limits:
      memory: "1Gi"
      cpu: "1000m"
```

### Adjust Message Size

Test with different message sizes:

```yaml
performanceTest:
  messageSize: 2048  # 2KB messages
  # or
  messageSize: 10240  # 10KB messages
```

### Extend Keep-Alive Time

For longer metric scraping:

```yaml
performanceTest:
  keepAliveMinutes: 10  # Keep running 10 minutes after test
```

## Cleanup

### Manual Cleanup

```bash
# Delete specific test run
kubectl delete job -n demo mq-performance-test-manual

# Delete all performance test jobs
kubectl delete jobs -n demo -l app=mq-performance-test
```

### Automatic Cleanup

Jobs are automatically deleted after `ttlAfterFinished` seconds (default: 1 hour).

## Local Development

### Run Locally (without Kubernetes)

Requires IBM MQ running on localhost:1414.

```bash
cd infrastructure/performance-test

# Set environment variables
set MESSAGE_COUNT=1000
set TEST_RUN_ID=local-test
set IBM_MQ_CONNNAME=localhost(1414)

# Run the application
mvnw.cmd spring-boot:run
```

Access metrics at http://localhost:8080/actuator/prometheus

## Architecture

```
┌─────────────────────────┐
│  Performance Test App   │
│  (Spring Boot + Java 25)│
└──────────┬──────────────┘
           │
           │ JMS
           ▼
┌─────────────────────────┐
│      IBM MQ (QM1)       │
│    DEV.QUEUE.1          │
└─────────────────────────┘

           ▲
           │ Scrape /actuator/prometheus
           │
┌─────────────────────────┐
│      Prometheus         │
│   (Every 15 seconds)    │
└──────────┬──────────────┘
           │
           │ Query
           ▼
┌─────────────────────────┐
│       Grafana           │
│  Performance Dashboard  │
└─────────────────────────┘
```

## Integration with Demo App

The performance test sends messages to the same queue (`DEV.QUEUE.1`) that the demo-app consumes from. You can observe:

1. **Messages sent** by performance test (perf_test_messages_sent_total)
2. **Messages received** by demo-app (mq_messages_received_total)
3. **Messages forwarded** to Kafka (kafka_messages_sent_total)

This allows end-to-end performance validation.

## Best Practices

1. **Run during maintenance windows** to avoid impacting production
2. **Start with small tests** (1,000 messages) to verify setup
3. **Monitor resource usage** during tests
4. **Clean up old jobs** to avoid cluttering the cluster
5. **Use unique run IDs** for tracking and comparison

## Example Test Scenarios

### Baseline Test
```batch
run-test.bat 10000 baseline-v1.0
```

### Load Test
```batch
run-test.bat 50000 load-test-50k
```

### Stress Test
```batch
run-test.bat 100000 stress-test-100k
```

### Large Message Test
Edit values.yaml:
```yaml
performanceTest:
  messageCount: 10000
  messageSize: 10240  # 10KB messages
```

Then run:
```batch
run-test.bat 10000 large-msg-10kb
```

## Support

For issues or questions:
1. Check logs: `kubectl logs -n demo -l app=mq-performance-test -f`
2. View pod status: `kubectl describe pod -n demo -l app=mq-performance-test`
3. Check Prometheus targets: http://localhost:31090/targets
4. Verify MQ connectivity: `kubectl exec -n demo -it <demo-app-pod> -- nc -zv ibmmq 1414`
