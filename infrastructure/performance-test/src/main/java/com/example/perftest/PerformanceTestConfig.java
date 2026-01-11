package com.example.perftest;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the performance test.
 * Values can be overridden via environment variables or application.yaml.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "performance-test")
public class PerformanceTestConfig {

    /**
     * Unique identifier for this test run.
     */
    private String testRunId = "manual";

    /**
     * Number of messages to send to MQ.
     */
    private int messageCount = 10000;

    /**
     * Size of each message in bytes.
     */
    private int messageSize = 1024;

    /**
     * How long to keep the application alive after test completion (in minutes).
     * This allows Prometheus to scrape the metrics.
     */
    private int keepAliveMinutes = 5;

    /**
     * Queue manager name (from ibm.mq configuration).
     */
    private String queueManager;

    /**
     * Queue name (from ibm.mq configuration).
     */
    private String queueName;
}
