package com.example.demo.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MqToKafkaMessageConverterTest {

    private MqToKafkaMessageConverter converter;

    @BeforeEach
    void setUp() {
        converter = new MqToKafkaMessageConverter();
    }

    @Test
    void shouldPassThroughMessageUnchanged() {
        String testMessage = "Test message from MQ";

        String result = converter.convert(testMessage);

        assertThat(result).isEqualTo(testMessage);
    }

    @Test
    void shouldPassThroughIso8583Message() {
        String isoMessage =
                "0200" +
                "1234567890123456" +
                "000000" +
                "000000010000" +
                "0101120000" +
                "123456" +
                "120000" +
                "0101" +
                "5411" +
                "123456" +
                "123456789012" +
                "AUTH12" +
                "00" +
                "TERM0001" +
                "MERCHANT000001" +
                "840" +
                "Test data";

        String result = converter.convert(isoMessage);

        assertThat(result).isEqualTo(isoMessage);
    }

    @Test
    void shouldHandleEmptyString() {
        String emptyMessage = "";

        String result = converter.convert(emptyMessage);

        assertThat(result).isEqualTo(emptyMessage);
    }

    @Test
    void shouldHandleLongMessage() {
        String longMessage = "X".repeat(10000);

        String result = converter.convert(longMessage);

        assertThat(result).isEqualTo(longMessage);
    }

    @Test
    void shouldHandleSpecialCharacters() {
        String specialMessage = "Special chars: @#$%^&*(){}[]|\\:;\"'<>,.?/~`";

        String result = converter.convert(specialMessage);

        assertThat(result).isEqualTo(specialMessage);
    }
}
