package com.example.demo.config;

import com.example.demo.service.KafkaMessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class KafkaConfig {

    @Bean
    public KafkaMessageService kafkaMessageService(
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${kafka.topic.name}") String kafkaTopic) {
        return new KafkaMessageService(kafkaTemplate, kafkaTopic);
    }
}
