package com.example.demo.listener;

import com.example.demo.converter.MqToKafkaMessageConverter;
import com.example.demo.service.KafkaMessageService;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MessageListener {

    private final KafkaMessageService kafkaMessageService;
    private final MqToKafkaMessageConverter messageConverter;

    public MessageListener(KafkaMessageService kafkaMessageService,
                           MqToKafkaMessageConverter messageConverter) {
        this.kafkaMessageService = kafkaMessageService;
        this.messageConverter = messageConverter;
    }

    @JmsListener(destination = "${ibm.mq.queue-name}")
    @Counted(value = "mq.messages.received", description = "Total number of messages received from MQ")
    @Timed(value = "message.processing.time",
            description = "Time taken to process and forward message from MQ to Kafka")
    public void receiveMessage(String message) {
        log.info("Received message from MQ: {}", message);

        String kafkaMessage = messageConverter.convert(message);
        kafkaMessageService.sendMessage(kafkaMessage);

        log.info("Forwarded message to Kafka: {}", kafkaMessage);
    }
}
