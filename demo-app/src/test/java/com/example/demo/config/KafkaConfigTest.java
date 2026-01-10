package com.example.demo.config;

import com.example.demo.service.KafkaMessageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class KafkaConfigTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void shouldCreateKafkaMessageService() {
        KafkaConfig kafkaConfig = new KafkaConfig();
        String kafkaTopic = "test-topic";

        KafkaMessageService service = kafkaConfig.kafkaMessageService(kafkaTemplate, kafkaTopic);

        assertThat(service).isNotNull();
    }
}
