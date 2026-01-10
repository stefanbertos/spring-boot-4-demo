package com.example.demo.listener;

import com.example.demo.converter.MqToKafkaMessageConverter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MessageListener {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final MqToKafkaMessageConverter messageConverter;
    private final Counter messagesReceivedCounter;
    private final Counter messagesForwardedCounter;
    private final Timer messageProcessingTimer;

    @Value("${kafka.topic.name}")
    private String kafkaTopic;

    public MessageListener(KafkaTemplate<String, String> kafkaTemplate,
                          MqToKafkaMessageConverter messageConverter,
                          MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.messageConverter = messageConverter;

        this.messagesReceivedCounter = Counter.builder("mq.messages.received")
                .description("Total number of messages received from MQ")
                .register(meterRegistry);

        this.messagesForwardedCounter = Counter.builder("kafka.messages.forwarded")
                .description("Total number of messages forwarded to Kafka")
                .register(meterRegistry);

        this.messageProcessingTimer = Timer.builder("message.processing.time")
                .description("Time taken to process and forward message from MQ to Kafka")
                .register(meterRegistry);
    }

    @JmsListener(destination = "${ibm.mq.queue-name}")
    public void receiveMessage(String message) {
        messageProcessingTimer.record(() -> {
            log.info("Received message from MQ: {}", message);
            messagesReceivedCounter.increment();

            String kafkaMessage = messageConverter.convert(message);
            kafkaTemplate.send(kafkaTopic, kafkaMessage);
            messagesForwardedCounter.increment();

            log.info("Forwarded message to Kafka topic '{}': {}", kafkaTopic, kafkaMessage);
        });
    }
}
