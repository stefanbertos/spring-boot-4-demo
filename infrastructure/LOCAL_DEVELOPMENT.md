# Local Development Guide

## Running the Spring Boot App Locally

When running the Spring Boot application on your local machine (outside Kubernetes), you need to connect to services running in Kubernetes.

### Quick Start

1. **Deploy infrastructure to Kubernetes:**
   ```cmd
   cd infrastructure\helm
   deploy.bat deploy --infra-only
   ```

2. **Start port forwarding:**
   ```cmd
   cd infrastructure
   port-forward.bat
   ```
   This forwards:
   - Kafka: `localhost:9092`
   - IBM MQ: `localhost:1414`

3. **Run your Spring Boot app:**
   ```cmd
   cd demo-app
   mvn spring-boot:run
   ```

### Access URLs

**Services requiring port-forward (for app connection):**
- Kafka: `localhost:9092`
- IBM MQ: `localhost:1414`

**Services already accessible (no port-forward needed):**
- Prometheus: http://localhost:31090
- Grafana: http://localhost:31300 (admin/admin)
- Kafka UI: http://localhost:31800
- IBM MQ Console: https://localhost:31443 (admin/passw0rd)

### Stop Port Forwarding

To stop all port forwards:
```cmd
cd infrastructure
stop-port-forward.bat
```

Or close the minimized PowerShell windows manually.

### Alternative: Use NodePort for IBM MQ

Instead of port-forwarding IBM MQ to 1414, you can configure your app to use the NodePort:

Change in `application.yaml`:
```yaml
ibm:
  mq:
    conn-name: localhost(31414)  # Use NodePort instead of 1414
```

Then you only need to port-forward Kafka.

### Troubleshooting

**Connection refused errors:**
- Ensure port-forward.bat is running
- Check that services are deployed: `kubectl get pods -n demo`
- Verify port forwards: `kubectl get svc -n demo`

**Port already in use:**
- Stop existing port forwards: `stop-port-forward.bat`
- Check for processes using ports: `netstat -ano | findstr "9092 1414"`
