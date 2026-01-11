package com.example.demo.listener;

import com.example.demo.config.TestcontainersConfiguration;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class MessageListenerIntegrationTest {

    @Autowired
    private JmsTemplate jmsTemplate;

    @Value("${ibm.mq.queue-name}")
    private String queueName;

    @Value("${kafka.topic.name}")
    private String kafkaTopic;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private KafkaMessageListenerContainer<String, String> container;
    private BlockingQueue<ConsumerRecord<String, String>> records;

    @BeforeEach
    void setUp() {
        records = new LinkedBlockingQueue<>();

        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        DefaultKafkaConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);
        ContainerProperties containerProperties = new ContainerProperties(kafkaTopic);

        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        container.setupMessageListener((MessageListener<String, String>) records::add);
        container.start();

        ContainerTestUtils.waitForAssignment(container, 1);
    }

    @AfterEach
    void tearDown() {
        if (container != null) {
            container.stop();
        }
    }

    @Test
    void shouldConsumeMessageFromMQAndForwardToKafka() throws Exception {
        int messageCount = 10;

        // Store sent messages for verification
        String[] sentMessages = new String[messageCount];

        for (int i = 0; i < messageCount; i++) {
            // Create ISO 8583 formatted message
            String isoMessage =
                    "0200" +                          // MTI
                    "1234567890123456" +              // PAN
                    "000000" +                        // Processing Code
                    "000000010000" +                  // Amount (100.00)
                    "0101120000" +                    // Transmission DateTime
                    String.format("%06d", i) +        // STAN
                    "120000" +                        // Local Time
                    "0101" +                          // Local Date
                    "5411" +                          // Merchant Type
                    "123456" +                        // Acquiring Institution Code
                    "123456789012" +                  // Retrieval Reference Number
                    String.format("AUTH%02d", i) +    // Authorization ID (padded to 6)
                    "00" +                            // Response Code
                    "TERM0001" +                      // Terminal ID
                    "MERCHANT000001" +                // Merchant ID
                    "840" +                           // Currency Code
                    "Test message " + i;              // Additional Data

            sentMessages[i] = isoMessage;
            jmsTemplate.convertAndSend(queueName, isoMessage);
        }

        // Verify messages are forwarded as-is (pass-through)
        for (int i = 0; i < messageCount; i++) {
            ConsumerRecord<String, String> record = records.poll(60, TimeUnit.SECONDS);
            assertThat(record).isNotNull();
            assertThat(record.value()).isEqualTo(sentMessages[i]);
        }
    }

    @Test
    void shouldHandleSingleMessage() throws Exception {
        // Create ISO 8583 formatted message
        String isoMessage =
                "0200" +                          // MTI
                "9876543210987654" +              // PAN
                "000000" +                        // Processing Code
                "000000025050" +                  // Amount (250.50)
                "0101120000" +                    // Transmission DateTime
                "999999" +                        // STAN
                "120000" +                        // Local Time
                "0101" +                          // Local Date
                "5411" +                          // Merchant Type
                "654321" +                        // Acquiring Institution Code
                "210987654321" +                  // Retrieval Reference Number
                "AUTH99" +                        // Authorization ID
                "00" +                            // Response Code
                "TERM9999" +                      // Terminal ID
                "MERCHANT999999" +                // Merchant ID
                "840" +                           // Currency Code
                "Single test message";            // Additional Data

        jmsTemplate.convertAndSend(queueName, isoMessage);

        ConsumerRecord<String, String> record = records.poll(30, TimeUnit.SECONDS);
        assertThat(record).isNotNull();

        // Verify message is passed through unchanged
        assertThat(record.value()).isEqualTo(isoMessage);
    }

    @Test
    void shouldHandleTextMessage() throws Exception {
        String textMessage = "Simple text message from MQ";

        jmsTemplate.convertAndSend(queueName, textMessage);

        ConsumerRecord<String, String> record = records.poll(30, TimeUnit.SECONDS);
        assertThat(record).isNotNull();
        assertThat(record.value()).isEqualTo(textMessage);
    }
}
