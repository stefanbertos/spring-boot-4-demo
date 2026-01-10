package com.example.demo.converter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Converter for MQ to Kafka messages.
 * Currently performs a pass-through operation - messages are forwarded as-is from MQ to Kafka.
 * The ISO 8583 parser is available if parsing/transformation is needed in the future.
 */
@Slf4j
@Component
public class MqToKafkaMessageConverter implements Converter<String, String> {

    @Override
    public String convert(String mqMessage) {
        log.debug("Forwarding message from MQ to Kafka: {}", mqMessage);
        // Pass through - no conversion needed
        return mqMessage;
    }
}
