package com.example.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaMessageServiceTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private KafkaMessageService kafkaMessageService;

    private static final String KAFKA_TOPIC = "test-topic";

    @BeforeEach
    void setUp() {
        kafkaMessageService = new KafkaMessageService(kafkaTemplate, KAFKA_TOPIC);
    }

    @Test
    void shouldSendMessageToKafka() {
        String testMessage = "test message";

        kafkaMessageService.sendMessage(testMessage);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), messageCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo(KAFKA_TOPIC);
        assertThat(messageCaptor.getValue()).isEqualTo(testMessage);
    }

    @Test
    void shouldSendMultipleMessages() {
        String message1 = "message 1";
        String message2 = "message 2";

        kafkaMessageService.sendMessage(message1);
        kafkaMessageService.sendMessage(message2);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        verify(kafkaTemplate).send(KAFKA_TOPIC, message1);
        verify(kafkaTemplate).send(KAFKA_TOPIC, message2);
    }
}
