package com.example.demo.parser;

import com.example.demo.dto.BackIsoTransactionMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Iso8583ParserTest {

    private Iso8583Parser parser;

    @BeforeEach
    void setUp() {
        parser = new Iso8583Parser();
    }

    @Test
    void shouldParseValidIso8583Message() {
        // MTI(4) + PAN(16) + ProcCode(6) + Amount(12) + DateTime(10) + STAN(6) + LocalTime(6) + LocalDate(4)
        // + MerchType(4) + AcqInst(6) + RetrievalRef(12) + AuthId(6) + RespCode(2) + TermId(8)
        // + MerchId(15) + CurrCode(3) + AdditionalData
        String isoMessage =
                "0200" +                          // MTI
                "1234567890123456" +              // PAN
                "000000" +                        // Processing Code
                "000000010000" +                  // Amount (100.00)
                "0101120000" +                    // Transmission DateTime (Jan 1, 12:00:00)
                "123456" +                        // STAN
                "120000" +                        // Local Time
                "0101" +                          // Local Date
                "5411" +                          // Merchant Type
                "123456" +                        // Acquiring Institution Code
                "123456789012" +                  // Retrieval Reference Number
                "AUTH12" +                        // Authorization ID
                "00" +                            // Response Code
                "TERM0001" +                      // Terminal ID
                "MERCHANT000001" +                // Merchant ID
                "840" +                           // Currency Code (USD)
                "Additional data here";           // Additional Data

        BackIsoTransactionMessage result = parser.parse(isoMessage);

        assertThat(result).isNotNull();
        assertThat(result.messageTypeIndicator()).isEqualTo("0200");
        assertThat(result.primaryAccountNumber()).isEqualTo("1234567890123456");
        assertThat(result.processingCode()).isEqualTo("000000");
        assertThat(result.transactionAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(result.systemTraceAuditNumber()).isEqualTo("123456");
        assertThat(result.localTransactionTime()).isEqualTo("120000");
        assertThat(result.localTransactionDate()).isEqualTo("0101");
        assertThat(result.merchantType()).isEqualTo("5411");
        assertThat(result.acquiringInstitutionCode()).isEqualTo("123456");
        assertThat(result.retrievalReferenceNumber()).isEqualTo("123456789012");
        assertThat(result.authorizationIdResponse()).isEqualTo("AUTH12");
        assertThat(result.responseCode()).isEqualTo("00");
        assertThat(result.terminalId()).isEqualTo("TERM0001");
        assertThat(result.merchantId()).isEqualTo("MERCHANT000001");
        assertThat(result.currencyCode()).isEqualTo("840");
        assertThat(result.additionalData()).isEqualTo("Additional data here");
    }

    @Test
    void shouldParseMessageWithMinimalAdditionalData() {
        String isoMessage =
                "0210" +                          // MTI
                "9876543210987654" +              // PAN
                "000000" +                        // Processing Code
                "000000025050" +                  // Amount (250.50)
                "1215143000" +                    // Transmission DateTime (Dec 15, 14:30:00)
                "654321" +                        // STAN
                "143000" +                        // Local Time
                "1215" +                          // Local Date
                "5812" +                          // Merchant Type
                "654321" +                        // Acquiring Institution Code
                "210987654321" +                  // Retrieval Reference Number
                "AUTH99" +                        // Authorization ID
                "00" +                            // Response Code
                "TERM9999" +                      // Terminal ID
                "MERCHANT999999" +                // Merchant ID
                "978";                            // Currency Code (EUR)

        BackIsoTransactionMessage result = parser.parse(isoMessage);

        assertThat(result).isNotNull();
        assertThat(result.messageTypeIndicator()).isEqualTo("0210");
        assertThat(result.transactionAmount()).isEqualTo(new BigDecimal("250.50"));
        assertThat(result.additionalData()).isEmpty();
    }

    @Test
    void shouldHandleAmountWithLeadingZeros() {
        String isoMessage =
                "0200" +
                "1234567890123456" +
                "000000" +
                "000000000100" +                  // Amount (1.00)
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
                "840";

        BackIsoTransactionMessage result = parser.parse(isoMessage);

        assertThat(result.transactionAmount()).isEqualTo(new BigDecimal("1.00"));
    }

    @Test
    void shouldHandleZeroAmount() {
        String isoMessage =
                "0200" +
                "1234567890123456" +
                "000000" +
                "000000000000" +                  // Amount (0.00)
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
                "840";

        BackIsoTransactionMessage result = parser.parse(isoMessage);

        assertThat(result.transactionAmount()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void shouldThrowExceptionForNullMessage() {
        assertThatThrownBy(() -> parser.parse(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
    }

    @Test
    void shouldThrowExceptionForEmptyMessage() {
        assertThatThrownBy(() -> parser.parse(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
    }

    @Test
    void shouldThrowExceptionForMessageTooShort() {
        String shortMessage = "0200123456"; // Way too short

        assertThatThrownBy(() -> parser.parse(shortMessage))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to parse ISO 8583 message");
    }

    @Test
    void shouldTrimFieldValues() {
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
                "840";

        BackIsoTransactionMessage result = parser.parse(isoMessage);

        // All fields should be trimmed
        assertThat(result.messageTypeIndicator()).doesNotContain(" ");
        assertThat(result.primaryAccountNumber()).doesNotContain(" ");
        assertThat(result.processingCode()).doesNotContain(" ");
    }
}
