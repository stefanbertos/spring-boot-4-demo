package com.example.demo;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
class MessageListenerIntegrationTest {

    @Autowired
    private JmsTemplate jmsTemplate;

    @Value("${ibm.mq.queue-name}")
    private String queueName;

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        Logger logger = (Logger) LoggerFactory.getLogger(MessageListener.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @Test
    void shouldConsumeMessageFromQueueAndLogIt() {
        String testMessage = "Hello from Integration Test";

        jmsTemplate.convertAndSend(queueName, testMessage);

        await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(listAppender.list)
                            .extracting(ILoggingEvent::getFormattedMessage)
                            .anyMatch(msg -> msg.contains("Received message from queue: " + testMessage));
                });
    }
}
