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
│   ├── docker-compose.yml  # All services (MQ, Kafka, Prometheus, Grafana)
│   ├── prometheus.yml      # Prometheus configuration
│   ├── grafana/            # Grafana provisioning
│   └── README.md           # Infrastructure documentation
└── demo-app/               # Main application module
    ├── pom.xml
    └── src/
        ├── main/java/
        └── test/java/
```

## Features

- **Message Bridging**: Consumes messages from IBM MQ and forwards to Apache Kafka
- **ISO 8583 Support**: Handles back ISO transaction messages with proper DTO mapping
- **Metrics & Observability**:
  - Micrometer metrics with annotations (@Counted, @Timed)
  - Prometheus integration for metrics collection
  - Pre-configured Grafana dashboards
- **Virtual Threads**: Leverages Java 25 virtual threads for improved concurrency
- **100% Test Coverage**: Comprehensive unit and integration tests

## Technology Stack

- **Java 25** with virtual threads
- **Spring Boot 4.0.1**
- **IBM MQ 4.0.1**
- **Apache Kafka** (latest)
- **Micrometer + Prometheus** for metrics
- **Grafana** for visualization
- **JUnit 5 + Mockito** for testing

## Quick Start

### Prerequisites

- Java 25
- Maven 3.9+
- Docker and Docker Compose

### 1. Start Infrastructure

```bash
cd infrastructure
docker-compose up -d
```

This starts:
- IBM MQ (ports 1414, 9443)
- Apache Kafka (port 9092)
- Prometheus (port 9090)
- Grafana (port 3000)

### 2. Build the Application

```bash
mvn clean install
```

### 3. Run the Application

```bash
cd demo-app
mvn spring-boot:run
```

Or run the JAR:

```bash
java -jar demo-app/target/demo-app-0.0.1-SNAPSHOT.jar
```

### 4. Verify Setup

- Application health: http://localhost:8080/actuator/health
- Prometheus metrics: http://localhost:8080/actuator/prometheus
- Grafana dashboard: http://localhost:3000 (admin/admin)
- IBM MQ console: https://localhost:9443/ibmmq/console (admin/passw0rd)

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

## Message Format

The application forwards messages from IBM MQ to Apache Kafka **as-is** without any transformation. The converter performs a pass-through operation.

### Example: ISO 8583 Banking Message

```
02001234567890123456000000000000010000010112000012345612000001015411123456123456789012AUTH1200TERM0001MERCHANT000001840Additional data
```

The application includes an ISO 8583 parser utility (`Iso8583Parser`) that can be used if message parsing is needed in the future. The `BackIsoTransactionMessage` DTO is also available for structured message handling.

### Message Flow

1. Message arrives at IBM MQ queue
2. MessageListener consumes the message
3. Converter passes the message through unchanged
4. KafkaMessageService forwards the message to Kafka topic
5. Message arrives at Kafka in the exact same format as it was in MQ

## Configuration

Main configuration is in `demo-app/src/main/resources/application.yaml`:

- IBM MQ connection settings
- Kafka bootstrap servers
- Topic names
- Actuator endpoints

## Metrics

Available metrics:
- `mq.messages.received` - Counter for MQ messages received
- `kafka.messages.sent` - Counter for Kafka messages sent
- `message.processing.time` - Timer for end-to-end processing time

Access metrics at: http://localhost:8080/actuator/metrics

## Development Guidelines

See [GUIDELINES.md](GUIDELINES.md) for comprehensive development standards including:
- Constructor-based dependency injection
- Virtual threads usage
- Java records for DTOs
- 100% test coverage requirements
- Code quality standards

## Troubleshooting

### Application won't connect to MQ
- Verify MQ is running: `docker-compose ps ibmmq`
- Check MQ logs: `docker-compose logs ibmmq`
- Ensure queue DEV.QUEUE.1 exists in MQ console

### Messages not appearing in Kafka
- Check Kafka logs: `docker-compose logs kafka`
- Verify topic creation: `docker-compose exec kafka kafka-topics.sh --list --bootstrap-server localhost:9092`
- Review application logs for errors

### Metrics not in Prometheus/Grafana
- Verify app metrics endpoint: http://localhost:8080/actuator/prometheus
- Check Prometheus targets: http://localhost:9090/targets
- Ensure Prometheus can reach `host.docker.internal:8080`

## License

This project is for demonstration purposes.
