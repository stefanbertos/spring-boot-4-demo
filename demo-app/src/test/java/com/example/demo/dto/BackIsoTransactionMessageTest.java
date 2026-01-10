package com.example.demo.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BackIsoTransactionMessageTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldCreateBackIsoTransactionMessage() {
        LocalDateTime now = LocalDateTime.now();

        BackIsoTransactionMessage message = new BackIsoTransactionMessage(
                "0200",
                "1234567890123456",
                "000000",
                new BigDecimal("100.00"),
                now,
                "123456",
                "123456",
                "0101",
                "5411",
                "123456",
                "123456789012",
                "AUTH123",
                "00",
                "TERM001",
                "MERCH001",
                "840",
                "Additional data"
        );

        assertThat(message).isNotNull();
        assertThat(message.messageTypeIndicator()).isEqualTo("0200");
        assertThat(message.primaryAccountNumber()).isEqualTo("1234567890123456");
        assertThat(message.processingCode()).isEqualTo("000000");
        assertThat(message.transactionAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(message.transmissionDateTime()).isEqualTo(now);
        assertThat(message.systemTraceAuditNumber()).isEqualTo("123456");
        assertThat(message.localTransactionTime()).isEqualTo("123456");
        assertThat(message.localTransactionDate()).isEqualTo("0101");
        assertThat(message.merchantType()).isEqualTo("5411");
        assertThat(message.acquiringInstitutionCode()).isEqualTo("123456");
        assertThat(message.retrievalReferenceNumber()).isEqualTo("123456789012");
        assertThat(message.authorizationIdResponse()).isEqualTo("AUTH123");
        assertThat(message.responseCode()).isEqualTo("00");
        assertThat(message.terminalId()).isEqualTo("TERM001");
        assertThat(message.merchantId()).isEqualTo("MERCH001");
        assertThat(message.currencyCode()).isEqualTo("840");
        assertThat(message.additionalData()).isEqualTo("Additional data");
    }

    @Test
    void shouldSerializeToJson() throws Exception {
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 12, 0, 0);

        BackIsoTransactionMessage message = new BackIsoTransactionMessage(
                "0200",
                "1234567890123456",
                "000000",
                new BigDecimal("100.00"),
                now,
                "123456",
                "123456",
                "0101",
                "5411",
                "123456",
                "123456789012",
                "AUTH123",
                "00",
                "TERM001",
                "MERCH001",
                "840",
                "Additional data"
        );

        String json = objectMapper.writeValueAsString(message);

        assertThat(json).isNotEmpty();
        assertThat(json).contains("\"mti\":\"0200\"");
        assertThat(json).contains("\"pan\":\"1234567890123456\"");
        assertThat(json).contains("\"responseCode\":\"00\"");
    }

    @Test
    void shouldDeserializeFromJson() throws Exception {
        String json = """
                {
                    "mti": "0200",
                    "pan": "1234567890123456",
                    "processingCode": "000000",
                    "transactionAmount": 100.00,
                    "transmissionDateTime": "2024-01-01T12:00:00",
                    "stan": "123456",
                    "localTransactionTime": "123456",
                    "localTransactionDate": "0101",
                    "merchantType": "5411",
                    "acquiringInstitutionCode": "123456",
                    "retrievalReferenceNumber": "123456789012",
                    "authorizationIdResponse": "AUTH123",
                    "responseCode": "00",
                    "terminalId": "TERM001",
                    "merchantId": "MERCH001",
                    "currencyCode": "840",
                    "additionalData": "Additional data"
                }
                """;

        BackIsoTransactionMessage message = objectMapper.readValue(json, BackIsoTransactionMessage.class);

        assertThat(message).isNotNull();
        assertThat(message.messageTypeIndicator()).isEqualTo("0200");
        assertThat(message.primaryAccountNumber()).isEqualTo("1234567890123456");
        assertThat(message.responseCode()).isEqualTo("00");
        assertThat(message.transactionAmount()).isEqualTo(new BigDecimal("100.00"));
    }
}
