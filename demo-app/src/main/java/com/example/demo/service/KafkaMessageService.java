package com.example.demo.service;

import io.micrometer.core.annotation.Counted;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Map;

@Slf4j
public class KafkaMessageService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String kafkaTopic;

    public KafkaMessageService(KafkaTemplate<String, String> kafkaTemplate,
                               String kafkaTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaTopic = kafkaTopic;
    }

    /**
     * Sends a message to Kafka without headers (backward compatibility).
     */
    @Counted(value = "kafka.messages.sent", description = "Total number of messages sent to Kafka")
    public void sendMessage(String message) {
        log.info("Sending message to Kafka topic '{}': {}", kafkaTopic, message);
        kafkaTemplate.send(kafkaTopic, message);
    }

    /**
     * Sends a message to Kafka with custom headers.
     * Headers are preserved from JMS for performance test correlation.
     */
    @Counted(value = "kafka.messages.sent", description = "Total number of messages sent to Kafka")
    public void sendMessage(String messagePayload, Map<String, String> headers) {
        log.info("Sending message to Kafka topic '{}' with {} headers: {}",
                kafkaTopic, headers.size(), messagePayload);

        // Build Spring messaging Message with headers
        MessageBuilder<String> messageBuilder = MessageBuilder
                .withPayload(messagePayload)
                .setHeader(KafkaHeaders.TOPIC, kafkaTopic);

        // Add custom headers from JMS
        headers.forEach((key, value) -> {
            messageBuilder.setHeader(key, value);
            log.debug("Adding header: {}={}", key, value);
        });

        Message<String> message = messageBuilder.build();

        // Send to Kafka
        kafkaTemplate.send(message);
    }
}
