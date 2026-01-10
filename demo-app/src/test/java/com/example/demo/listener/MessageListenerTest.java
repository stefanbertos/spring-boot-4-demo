package com.example.demo.listener;

import com.example.demo.converter.MqToKafkaMessageConverter;
import com.example.demo.service.KafkaMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageListenerTest {

    @Mock
    private KafkaMessageService kafkaMessageService;

    @Mock
    private MqToKafkaMessageConverter messageConverter;

    private MessageListener messageListener;

    @BeforeEach
    void setUp() {
        messageListener = new MessageListener(kafkaMessageService, messageConverter);
    }

    @Test
    void shouldReceiveAndForwardMessage() {
        String mqMessage = "test MQ message";
        String kafkaMessage = "converted kafka message";

        when(messageConverter.convert(mqMessage)).thenReturn(kafkaMessage);

        messageListener.receiveMessage(mqMessage);

        verify(messageConverter).convert(mqMessage);
        verify(kafkaMessageService).sendMessage(kafkaMessage);
    }

    @Test
    void shouldHandleMultipleMessages() {
        String mqMessage1 = "message 1";
        String mqMessage2 = "message 2";
        String kafkaMessage1 = "kafka message 1";
        String kafkaMessage2 = "kafka message 2";

        when(messageConverter.convert(mqMessage1)).thenReturn(kafkaMessage1);
        when(messageConverter.convert(mqMessage2)).thenReturn(kafkaMessage2);

        messageListener.receiveMessage(mqMessage1);
        messageListener.receiveMessage(mqMessage2);

        verify(messageConverter).convert(mqMessage1);
        verify(messageConverter).convert(mqMessage2);
        verify(kafkaMessageService).sendMessage(kafkaMessage1);
        verify(kafkaMessageService).sendMessage(kafkaMessage2);
    }
}
