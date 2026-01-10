package com.example.demo.service;

import io.micrometer.core.annotation.Counted;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaMessageService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String kafkaTopic;

    public KafkaMessageService(KafkaTemplate<String, String> kafkaTemplate,
                               String kafkaTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaTopic = kafkaTopic;
    }

    @Counted(value = "kafka.messages.sent", description = "Total number of messages sent to Kafka")
    public void sendMessage(String message) {
        log.info("Sending message to Kafka topic '{}': {}", kafkaTopic, message);
        kafkaTemplate.send(kafkaTopic, message);
    }
}
