package com.example.perftest;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for tracking end-to-end performance metrics.
 * Tracks metrics for the complete flow: MQ send → Demo App → Kafka receive.
 */
@Slf4j
@Service
public class PerformanceMetricsService {

    private final MeterRegistry meterRegistry;
    private final PerformanceTestConfig config;

    // Counters
    private final Counter messagesReceivedCounter;
    private final Counter messagesLostCounter;

    // Timers
    private final Timer endToEndLatencyTimer;

    // Distribution summary for latency
    private final DistributionSummary endToEndLatencyDistribution;

    // Atomic trackers
    @Getter
    private final AtomicInteger totalMessagesReceived = new AtomicInteger(0);
    @Getter
    private final AtomicLong totalLatencyMs = new AtomicLong(0);
    @Getter
    private final AtomicLong minLatencyMs = new AtomicLong(Long.MAX_VALUE);
    @Getter
    private final AtomicLong maxLatencyMs = new AtomicLong(0);

    // Timestamps for throughput calculation
    @Getter
    private volatile Long firstMessageReceivedTime;
    @Getter
    private volatile Long lastMessageReceivedTime;

    public PerformanceMetricsService(MeterRegistry meterRegistry, PerformanceTestConfig config) {
        this.meterRegistry = meterRegistry;
        this.config = config;

        List<Tag> tags = List.of(
                Tag.of("test_run_id", config.getTestRunId()),
                Tag.of("queue", config.getQueueName())
        );

        // Initialize counters
        this.messagesReceivedCounter = Counter.builder("perf.test.kafka.messages.received")
                .description("Total number of messages received from Kafka")
                .tags(tags)
                .register(meterRegistry);

        this.messagesLostCounter = Counter.builder("perf.test.messages.lost")
                .description("Number of messages sent to MQ but not received from Kafka")
                .tags(tags)
                .register(meterRegistry);

        // Initialize timer for end-to-end latency
        this.endToEndLatencyTimer = Timer.builder("perf.test.end.to.end.latency")
                .description("End-to-end latency from MQ send to Kafka receive")
                .tags(tags)
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        // Initialize distribution summary
        this.endToEndLatencyDistribution = DistributionSummary.builder("perf.test.end.to.end.latency.distribution")
                .description("Distribution of end-to-end latency")
                .baseUnit("milliseconds")
                .tags(tags)
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        // Initialize gauges for real-time metrics
        meterRegistry.gauge("perf.test.messages.received.total", tags, totalMessagesReceived);
        meterRegistry.gauge("perf.test.latency.min.ms", tags, minLatencyMs);
        meterRegistry.gauge("perf.test.latency.max.ms", tags, maxLatencyMs);
        meterRegistry.gauge("perf.test.latency.avg.ms", tags, this, service -> {
            int received = service.totalMessagesReceived.get();
            return received > 0 ? (double) service.totalLatencyMs.get() / received : 0.0;
        });
    }

    /**
     * Records a message received from Kafka and calculates end-to-end latency.
     *
     * @param sentTimestamp When the message was sent to MQ (milliseconds)
     * @param receivedTimestamp When the message was received from Kafka (milliseconds)
     */
    public void recordMessageReceived(long sentTimestamp, long receivedTimestamp) {
        long latencyMs = receivedTimestamp - sentTimestamp;

        // Increment counter
        messagesReceivedCounter.increment();

        // Record latency
        endToEndLatencyTimer.record(latencyMs, TimeUnit.MILLISECONDS);
        endToEndLatencyDistribution.record(latencyMs);

        // Update atomic trackers
        int received = totalMessagesReceived.incrementAndGet();
        totalLatencyMs.addAndGet(latencyMs);

        // Update min/max latency
        minLatencyMs.updateAndGet(currentMin -> Math.min(currentMin, latencyMs));
        maxLatencyMs.updateAndGet(currentMax -> Math.max(currentMax, latencyMs));

        // Track first and last message timestamps for throughput calculation
        if (firstMessageReceivedTime == null) {
            synchronized (this) {
                if (firstMessageReceivedTime == null) {
                    firstMessageReceivedTime = receivedTimestamp;
                    log.info("First message received from Kafka");
                }
            }
        }
        lastMessageReceivedTime = receivedTimestamp;

        // Log progress every 1000 messages
        if (received % 1000 == 0) {
            double avgLatency = (double) totalLatencyMs.get() / received;
            log.info("Kafka Progress: {}/{} messages received, avg latency: {:.2f}ms",
                    received, config.getMessageCount(), avgLatency);
        }
    }

    /**
     * Records a lost message (sent to MQ but not received from Kafka).
     */
    public void recordMessageLost() {
        messagesLostCounter.increment();
    }

    /**
     * Calculates and records final metrics after test completion.
     */
    public void recordFinalMetrics() {
        int totalReceived = totalMessagesReceived.get();
        int totalSent = config.getMessageCount();
        int messagesLost = totalSent - totalReceived;

        if (messagesLost > 0) {
            log.warn("Message loss detected: {} messages sent but {} received (lost: {})",
                    totalSent, totalReceived, messagesLost);
            for (int i = 0; i < messagesLost; i++) {
                messagesLostCounter.increment();
            }
        }

        // Calculate end-to-end throughput
        if (firstMessageReceivedTime != null && lastMessageReceivedTime != null) {
            long durationMs = lastMessageReceivedTime - firstMessageReceivedTime;
            double durationSec = durationMs / 1000.0;
            double throughput = totalReceived / durationSec;

            // Record throughput as a gauge
            meterRegistry.gauge("perf.test.kafka.throughput",
                    List.of(
                            Tag.of("test_run_id", config.getTestRunId()),
                            Tag.of("queue", config.getQueueName())
                    ),
                    throughput);

            log.info("End-to-end throughput: {:.2f} messages/second", throughput);
        }

        // Calculate latency statistics
        if (totalReceived > 0) {
            double avgLatency = (double) totalLatencyMs.get() / totalReceived;

            log.info("=".repeat(80));
            log.info("End-to-End Performance Metrics:");
            log.info("  Messages sent to MQ: {}", totalSent);
            log.info("  Messages received from Kafka: {}", totalReceived);
            log.info("  Messages lost: {}", messagesLost);
            log.info("  Message loss rate: {:.2f}%", (messagesLost * 100.0 / totalSent));
            log.info("  Average end-to-end latency: {:.2f}ms", avgLatency);
            log.info("  Min latency: {}ms", minLatencyMs.get());
            log.info("  Max latency: {}ms", maxLatencyMs.get());
            log.info("=".repeat(80));
        } else {
            log.error("No messages received from Kafka! Test may have failed.");
        }
    }

    /**
     * Gets the current completion percentage.
     */
    public double getCompletionPercentage() {
        return (totalMessagesReceived.get() * 100.0) / config.getMessageCount();
    }

    /**
     * Checks if all messages have been received.
     */
    public boolean isComplete() {
        return totalMessagesReceived.get() >= config.getMessageCount();
    }
}
