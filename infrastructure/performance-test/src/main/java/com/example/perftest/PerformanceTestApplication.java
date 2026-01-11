package com.example.perftest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Duration;

/**
 * Spring Boot application for MQ performance testing.
 * Runs as a CommandLineRunner to send configurable number of messages to IBM MQ,
 * then keeps the application alive to expose Prometheus metrics.
 */
@Slf4j
@SpringBootApplication
public class PerformanceTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(PerformanceTestApplication.class, args);
    }

    /**
     * CommandLineRunner that executes the performance test on startup.
     */
    @Bean
    public CommandLineRunner performanceTestRunner(MqPerformanceService performanceService,
                                                    PerformanceTestConfig config) {
        return args -> {
            log.info("=".repeat(80));
            log.info("Starting MQ Performance Test");
            log.info("=".repeat(80));
            log.info("Configuration:");
            log.info("  Test Run ID: {}", config.getTestRunId());
            log.info("  Message Count: {}", config.getMessageCount());
            log.info("  Message Size: {} bytes", config.getMessageSize());
            log.info("  Queue Manager: {}", config.getQueueManager());
            log.info("  Queue Name: {}", config.getQueueName());
            log.info("=".repeat(80));

            // Execute the performance test
            performanceService.executePerformanceTest();

            log.info("=".repeat(80));
            log.info("Performance test completed successfully");
            log.info("Metrics available at: http://localhost:8080/actuator/prometheus");
            log.info("Application will remain running for {} to allow metrics scraping",
                    Duration.ofMinutes(config.getKeepAliveMinutes()));
            log.info("=".repeat(80));

            // Keep application alive for metrics scraping
            Thread.sleep(Duration.ofMinutes(config.getKeepAliveMinutes()).toMillis());

            log.info("Keep-alive period expired. Shutting down.");
        };
    }
}
