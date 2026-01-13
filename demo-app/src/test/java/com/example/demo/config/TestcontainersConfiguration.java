package com.example.demo.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Test configuration for Testcontainers.
 * Provides isolated IBM MQ and Kafka containers for integration tests.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    private static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0"))
            .withReuse(true);

    private static final GenericContainer<?> ibmMq = new GenericContainer<>(
            DockerImageName.parse("icr.io/ibm-messaging/mq:9.3.4.1-r1"))
            .withEnv("LICENSE", "accept")
            .withEnv("MQ_QMGR_NAME", "QM1")
            .withEnv("MQ_APP_PASSWORD", "passw0rd")
            .withEnv("MQ_ADMIN_PASSWORD", "passw0rd")
            .withExposedPorts(1414, 9443)
            .waitingFor(Wait.forListeningPorts(1414, 9443)
                    .withStartupTimeout(Duration.ofMinutes(3)))
            .withReuse(true);

    static {
        kafka.start();
        ibmMq.start();
    }

    /**
     * Dynamically configures Spring Boot properties from running containers.
     * This method is called before the ApplicationContext is loaded.
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Configure Kafka - override the default localhost:9092
        System.out.println("Configuring Kafka bootstrap-servers: " + kafka.getBootstrapServers());
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

        // Configure IBM MQ - override the default localhost(1414)
        String mqConnName = ibmMq.getHost() + "(" + ibmMq.getMappedPort(1414) + ")";
        System.out.println("Configuring IBM MQ conn-name: " + mqConnName);
        registry.add("ibm.mq.conn-name", () -> mqConnName);
    }

    /**
     * Exposes Kafka container as a bean for potential test usage.
     */
    @Bean
    public KafkaContainer kafkaContainer() {
        return kafka;
    }

    /**
     * Exposes IBM MQ container as a bean for potential test usage.
     */
    @Bean
    public GenericContainer<?> ibmMqContainer() {
        return ibmMq;
    }
}
