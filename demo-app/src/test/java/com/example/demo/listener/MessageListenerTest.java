package com.example.demo.listener;

import com.example.demo.converter.MqToKafkaMessageConverter;
import com.example.demo.service.KafkaMessageService;
import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
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
    void shouldReceiveAndForwardMessage() throws JMSException {
        String mqMessage = "test MQ message";
        String kafkaMessage = "converted kafka message";

        TextMessage textMessage = org.mockito.Mockito.mock(TextMessage.class);
        when(textMessage.getText()).thenReturn(mqMessage);

        when(messageConverter.convert(mqMessage)).thenReturn(kafkaMessage);

        messageListener.receiveMessage(textMessage);

        verify(messageConverter).convert(mqMessage);
        verify(kafkaMessageService).sendMessage(eq(kafkaMessage), anyMap());
    }

    @Test
    void shouldHandleMultipleMessages() throws JMSException {
        String mqMessage1 = "message 1";
        String mqMessage2 = "message 2";
        String kafkaMessage1 = "kafka message 1";
        String kafkaMessage2 = "kafka message 2";

        TextMessage textMessage1 = org.mockito.Mockito.mock(TextMessage.class);
        TextMessage textMessage2 = org.mockito.Mockito.mock(TextMessage.class);
        when(textMessage1.getText()).thenReturn(mqMessage1);
        when(textMessage2.getText()).thenReturn(mqMessage2);

        when(messageConverter.convert(mqMessage1)).thenReturn(kafkaMessage1);
        when(messageConverter.convert(mqMessage2)).thenReturn(kafkaMessage2);

        messageListener.receiveMessage(textMessage1);
        messageListener.receiveMessage(textMessage2);

        verify(messageConverter).convert(mqMessage1);
        verify(messageConverter).convert(mqMessage2);
        verify(kafkaMessageService).sendMessage(eq(kafkaMessage1), anyMap());
        verify(kafkaMessageService).sendMessage(eq(kafkaMessage2), anyMap());
    }
}
