package com.example.perftest;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Service for executing MQ performance tests.
 * Sends configurable number of messages to IBM MQ and tracks performance metrics.
 * Adds timestamps and correlation IDs to messages for end-to-end tracking.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MqPerformanceService {

    private final JmsTemplate jmsTemplate;
    private final PerformanceTestConfig config;
    private final MeterRegistry meterRegistry;
    private final PerformanceMetricsService metricsService;

    // Track sent message timestamps for end-to-end latency calculation
    private final ConcurrentHashMap<String, Long> sentMessageTimestamps = new ConcurrentHashMap<>();

    /**
     * Executes the performance test by sending messages to MQ.
     * Metrics are automatically tracked via @Counted and @Timed annotations.
     */
    public void executePerformanceTest() {
        Instant testStartTime = Instant.now();

        log.info("Beginning performance test: sending {} messages", config.getMessageCount());

        // Create a timer for the entire test duration
        Timer.Sample overallSample = Timer.start(meterRegistry);

        // Send messages
        for (int i = 0; i < config.getMessageCount(); i++) {
            String correlationId = generateCorrelationId(i);
            String message = generateMessage(i, correlationId);
            sendMessage(message, correlationId);

            // Log progress every 1000 messages
            if ((i + 1) % 1000 == 0) {
                log.info("Progress: {}/{} messages sent ({} %)",
                        i + 1, config.getMessageCount(),
                        String.format("%.1f", ((i + 1) * 100.0 / config.getMessageCount())));
            }
        }

        // Record overall test duration
        overallSample.stop(Timer.builder("perf.test.duration")
                .description("Total duration of the performance test")
                .tag("test_run_id", config.getTestRunId())
                .tag("queue", config.getQueueName())
                .register(meterRegistry));

        Instant testEndTime = Instant.now();
        Duration testDuration = Duration.between(testStartTime, testEndTime);
        double messagesPerSecond = config.getMessageCount() / (testDuration.toMillis() / 1000.0);

        // Record throughput as a gauge
        meterRegistry.gauge("perf.test.throughput",
                List.of(
                        Tag.of("test_run_id", config.getTestRunId()),
                        Tag.of("queue", config.getQueueName())
                ),
                messagesPerSecond);

        // Record test status (1 = completed successfully)
        meterRegistry.gauge("perf.test.status",
                List.of(
                        Tag.of("test_run_id", config.getTestRunId()),
                        Tag.of("queue", config.getQueueName())
                ),
                1.0);

        log.info("Performance test completed:");
        log.info("  Total messages sent: {}", config.getMessageCount());
        log.info("  Total duration: {} seconds", testDuration.toSeconds());
        log.info("  Average throughput: {:.2f} messages/second", messagesPerSecond);
    }

    /**
     * Sends a single message to MQ with metrics tracking.
     * Uses @Counted and @Timed for automatic metric collection.
     * Adds correlation ID and timestamp as JMS properties for end-to-end tracking.
     */
    @Counted(value = "perf.test.messages.sent",
            description = "Total number of messages sent to MQ during performance test")
    @Timed(value = "perf.test.message.send.time",
            description = "Time taken to send each individual message to MQ",
            percentiles = {0.5, 0.95, 0.99})
    private void sendMessage(String message, String correlationId) {
        long sendTimestamp = System.currentTimeMillis();

        // Store timestamp for correlation when message is received from Kafka
        sentMessageTimestamps.put(correlationId, sendTimestamp);

        // Send message with JMS properties for correlation and timestamp
        jmsTemplate.convertAndSend(config.getQueueName(), message, messagePostProcessor -> {
            messagePostProcessor.setStringProperty("correlationId", correlationId);
            messagePostProcessor.setLongProperty("sendTimestamp", sendTimestamp);
            messagePostProcessor.setStringProperty("testRunId", config.getTestRunId());
            return messagePostProcessor;
        });
    }

    /**
     * Gets the timestamp when a message was sent (for correlation).
     */
    public Long getSentTimestamp(String correlationId) {
        return sentMessageTimestamps.get(correlationId);
    }

    /**
     * Returns the map of all sent message timestamps.
     */
    public ConcurrentHashMap<String, Long> getSentMessageTimestamps() {
        return sentMessageTimestamps;
    }

    /**
     * Generates a correlation ID for message tracking.
     */
    private String generateCorrelationId(int messageNumber) {
        return String.format("%s-%010d", config.getTestRunId(), messageNumber);
    }

    /**
     * Generates a test message of the configured size.
     */
    private String generateMessage(int messageNumber, String correlationId) {
        // Create a message with the specified size
        StringBuilder messageBuilder = new StringBuilder();

        // Add header with message number, correlation ID, and metadata
        String header = String.format("MSG#%010d|CORR=%s|RUN=%s|SIZE=%d|",
                messageNumber,
                correlationId,
                config.getTestRunId(),
                config.getMessageSize());

        messageBuilder.append(header);

        // Fill the rest with padding to reach the desired message size
        int remainingSize = config.getMessageSize() - header.length();
        if (remainingSize > 0) {
            // Use a repeating pattern for the payload
            String pattern = "ABCDEFGHIJ";
            int fullPatterns = remainingSize / pattern.length();
            int remainder = remainingSize % pattern.length();

            for (int i = 0; i < fullPatterns; i++) {
                messageBuilder.append(pattern);
            }
            if (remainder > 0) {
                messageBuilder.append(pattern, 0, remainder);
            }
        }

        return messageBuilder.toString();
    }
}
