package com.example.demo.converter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MqToKafkaMessageConverter implements Converter<String, String> {

    @Override
    public String convert(String mqMessage) {
        log.debug("Converting MQ message to Kafka message: {}", mqMessage);
        return mqMessage;
    }
}
