# Infrastructure

This directory contains all infrastructure-related configuration files for local development and deployment.

## Contents

- `docker-compose.yml` - Docker Compose configuration for all infrastructure services
- `prometheus.yml` - Prometheus scraping configuration
- `grafana/` - Grafana provisioning configurations
  - `dashboards/` - Dashboard definitions
  - `datasources/` - Datasource configurations

## Services

### IBM MQ
- **Port**: 1414 (MQ), 9443 (Web Console)
- **Queue Manager**: QM1
- **Default Queue**: DEV.QUEUE.1
- **Credentials**: admin/passw0rd

### Apache Kafka
- **Port**: 9092
- **Mode**: KRaft (no Zookeeper required)
- **Topic**: mq-messages (auto-created)

### Prometheus
- **Port**: 9090
- **Scrape Interval**: 15s
- **Target**: Spring Boot application metrics endpoint

### Grafana
- **Port**: 3000
- **Credentials**: admin/admin
- **Pre-configured**: Prometheus datasource and MQ-Kafka dashboard

## Usage

### Start All Services
```bash
cd infrastructure
docker-compose up -d
```

### Stop All Services
```bash
cd infrastructure
docker-compose down
```

### View Logs
```bash
cd infrastructure
docker-compose logs -f [service-name]
```

### Clean Up Volumes (Reset Data)
```bash
cd infrastructure
docker-compose down -v
```

## Accessing Services

- **IBM MQ Web Console**: https://localhost:9443/ibmmq/console
- **Grafana**: http://localhost:3000
- **Prometheus**: http://localhost:9090

## Troubleshooting

### IBM MQ Connection Issues
1. Wait for MQ to fully start (check logs: `docker-compose logs ibmmq`)
2. Verify queue exists in the web console
3. Check connection credentials in `application.yaml`

### Kafka Connection Issues
1. Ensure Kafka is running: `docker-compose ps kafka`
2. Check logs: `docker-compose logs kafka`
3. Verify bootstrap server configuration

### Metrics Not Appearing in Prometheus
1. Verify Spring Boot app is running and accessible
2. Check Prometheus targets: http://localhost:9090/targets
3. Ensure actuator endpoints are exposed in `application.yaml`
