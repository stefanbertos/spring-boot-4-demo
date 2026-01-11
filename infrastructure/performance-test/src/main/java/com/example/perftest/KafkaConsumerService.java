package com.example.perftest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * Service for consuming messages from Kafka topic "mq-messages".
 * Correlates received messages with sent timestamps to calculate end-to-end latency.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final PerformanceMetricsService metricsService;
    private final MqPerformanceService mqService;
    private final PerformanceTestConfig config;

    /**
     * Listens to messages from the mq-messages Kafka topic.
     * Extracts correlation ID and timestamp from headers to calculate end-to-end latency.
     */
    @KafkaListener(
            topics = "${performance-test.kafka-topic}",
            groupId = "#{performanceTestConfig.testRunId}",
            autoStartup = "false",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeMessage(ConsumerRecord<String, String> record) {
        long receivedTimestamp = System.currentTimeMillis();

        try {
            // Extract headers from Kafka message (these come from demo-app forwarding)
            String correlationId = extractHeader(record, "correlationId");
            String sendTimestampStr = extractHeader(record, "sendTimestamp");
            String testRunId = extractHeader(record, "testRunId");

            // Validate this message belongs to our test run
            if (testRunId != null && !config.getTestRunId().equals(testRunId)) {
                log.debug("Ignoring message from different test run: {}", testRunId);
                return;
            }

            if (correlationId != null && sendTimestampStr != null) {
                try {
                    long sentTimestamp = Long.parseLong(sendTimestampStr);

                    // Record the message with end-to-end latency
                    metricsService.recordMessageReceived(sentTimestamp, receivedTimestamp);

                    log.debug("Received message {}, latency: {}ms",
                            correlationId, receivedTimestamp - sentTimestamp);
                } catch (NumberFormatException e) {
                    log.warn("Invalid sendTimestamp header: {}", sendTimestampStr, e);
                }
            } else {
                log.warn("Message missing required headers (correlationId or sendTimestamp)");
            }

            // Check if test is complete
            if (metricsService.isComplete()) {
                log.info("All messages received! Completion: {:.2f}%",
                        metricsService.getCompletionPercentage());
            }

        } catch (Exception e) {
            log.error("Error processing Kafka message", e);
        }
    }

    /**
     * Extracts a header value from the Kafka record.
     */
    private String extractHeader(ConsumerRecord<String, String> record, String headerName) {
        if (record.headers() != null) {
            org.apache.kafka.common.header.Header header = record.headers().lastHeader(headerName);
            if (header != null && header.value() != null) {
                return new String(header.value());
            }
        }
        return null;
    }
}
