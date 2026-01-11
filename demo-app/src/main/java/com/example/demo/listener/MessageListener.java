package com.example.demo.listener;

import com.example.demo.converter.MqToKafkaMessageConverter;
import com.example.demo.service.KafkaMessageService;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import java.util.HashMap;
import java.util.Map;

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
    public void receiveMessage(Message jmsMessage) throws JMSException {
        // Extract message body
        String messageBody = extractMessageBody(jmsMessage);
        log.info("Received message from MQ: {}", messageBody);

        // Extract JMS properties for performance test correlation
        Map<String, String> headers = extractHeaders(jmsMessage);

        // Convert message
        String kafkaMessage = messageConverter.convert(messageBody);

        // Send to Kafka with headers
        kafkaMessageService.sendMessage(kafkaMessage, headers);

        log.info("Forwarded message to Kafka: {}", kafkaMessage);
    }

    /**
     * Extracts the message body from JMS message.
     */
    private String extractMessageBody(Message jmsMessage) throws JMSException {
        if (jmsMessage instanceof TextMessage textMessage) {
            return textMessage.getText();
        }
        throw new IllegalArgumentException("Unsupported message type: " + jmsMessage.getClass());
    }

    /**
     * Extracts JMS properties as headers for Kafka.
     * Preserves performance test correlation data.
     */
    private Map<String, String> extractHeaders(Message jmsMessage) {
        Map<String, String> headers = new HashMap<>();

        try {
            // Extract performance test headers if present
            if (jmsMessage.propertyExists("correlationId")) {
                headers.put("correlationId", jmsMessage.getStringProperty("correlationId"));
            }
            if (jmsMessage.propertyExists("sendTimestamp")) {
                headers.put("sendTimestamp", String.valueOf(jmsMessage.getLongProperty("sendTimestamp")));
            }
            if (jmsMessage.propertyExists("testRunId")) {
                headers.put("testRunId", jmsMessage.getStringProperty("testRunId"));
            }

            log.debug("Extracted {} headers from JMS message", headers.size());
        } catch (JMSException e) {
            log.warn("Error extracting JMS properties", e);
        }

        return headers;
    }
}
