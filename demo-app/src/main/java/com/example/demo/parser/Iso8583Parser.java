package com.example.demo.parser;

import com.example.demo.dto.BackIsoTransactionMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Parser for ISO 8583 banking switch messages.
 *
 * This parser handles a simplified fixed-length ISO 8583 message format:
 * - MTI (4 chars): Message Type Indicator
 * - PAN (16 chars): Primary Account Number
 * - Processing Code (6 chars)
 * - Transaction Amount (12 chars)
 * - Transmission DateTime (10 chars - MMddHHmmss)
 * - STAN (6 chars): System Trace Audit Number
 * - Local Time (6 chars - HHmmss)
 * - Local Date (4 chars - MMdd)
 * - Merchant Type (4 chars)
 * - Acquiring Institution Code (6 chars)
 * - Retrieval Reference Number (12 chars)
 * - Authorization ID (6 chars)
 * - Response Code (2 chars)
 * - Terminal ID (8 chars)
 * - Merchant ID (15 chars)
 * - Currency Code (3 chars)
 * - Additional Data (variable length, remaining)
 */
@Slf4j
@Component
public class Iso8583Parser {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMddHHmmss");

    private static final int MTI_LENGTH = 4;
    private static final int PAN_LENGTH = 16;
    private static final int PROCESSING_CODE_LENGTH = 6;
    private static final int AMOUNT_LENGTH = 12;
    private static final int TRANSMISSION_DATETIME_LENGTH = 10;
    private static final int STAN_LENGTH = 6;
    private static final int LOCAL_TIME_LENGTH = 6;
    private static final int LOCAL_DATE_LENGTH = 4;
    private static final int MERCHANT_TYPE_LENGTH = 4;
    private static final int ACQUIRING_INST_LENGTH = 6;
    private static final int RETRIEVAL_REF_LENGTH = 12;
    private static final int AUTH_ID_LENGTH = 6;
    private static final int RESPONSE_CODE_LENGTH = 2;
    private static final int TERMINAL_ID_LENGTH = 8;
    private static final int MERCHANT_ID_LENGTH = 15;
    private static final int CURRENCY_CODE_LENGTH = 3;

    public BackIsoTransactionMessage parse(String isoMessage) {
        if (isoMessage == null || isoMessage.isEmpty()) {
            throw new IllegalArgumentException("ISO message cannot be null or empty");
        }

        log.debug("Parsing ISO 8583 message: {}", isoMessage);

        int position = 0;

        try {
            String mti = extractField(isoMessage, position, MTI_LENGTH);
            position += MTI_LENGTH;

            String pan = extractField(isoMessage, position, PAN_LENGTH);
            position += PAN_LENGTH;

            String processingCode = extractField(isoMessage, position, PROCESSING_CODE_LENGTH);
            position += PROCESSING_CODE_LENGTH;

            String amountStr = extractField(isoMessage, position, AMOUNT_LENGTH);
            BigDecimal amount = parseAmount(amountStr);
            position += AMOUNT_LENGTH;

            String transmissionDateTimeStr = extractField(isoMessage, position, TRANSMISSION_DATETIME_LENGTH);
            LocalDateTime transmissionDateTime = parseTransmissionDateTime(transmissionDateTimeStr);
            position += TRANSMISSION_DATETIME_LENGTH;

            String stan = extractField(isoMessage, position, STAN_LENGTH);
            position += STAN_LENGTH;

            String localTime = extractField(isoMessage, position, LOCAL_TIME_LENGTH);
            position += LOCAL_TIME_LENGTH;

            String localDate = extractField(isoMessage, position, LOCAL_DATE_LENGTH);
            position += LOCAL_DATE_LENGTH;

            String merchantType = extractField(isoMessage, position, MERCHANT_TYPE_LENGTH);
            position += MERCHANT_TYPE_LENGTH;

            String acquiringInstitutionCode = extractField(isoMessage, position, ACQUIRING_INST_LENGTH);
            position += ACQUIRING_INST_LENGTH;

            String retrievalReferenceNumber = extractField(isoMessage, position, RETRIEVAL_REF_LENGTH);
            position += RETRIEVAL_REF_LENGTH;

            String authorizationIdResponse = extractField(isoMessage, position, AUTH_ID_LENGTH);
            position += AUTH_ID_LENGTH;

            String responseCode = extractField(isoMessage, position, RESPONSE_CODE_LENGTH);
            position += RESPONSE_CODE_LENGTH;

            String terminalId = extractField(isoMessage, position, TERMINAL_ID_LENGTH);
            position += TERMINAL_ID_LENGTH;

            String merchantId = extractField(isoMessage, position, MERCHANT_ID_LENGTH);
            position += MERCHANT_ID_LENGTH;

            String currencyCode = extractField(isoMessage, position, CURRENCY_CODE_LENGTH);
            position += CURRENCY_CODE_LENGTH;

            String additionalData = position < isoMessage.length() ?
                    isoMessage.substring(position).trim() : "";

            BackIsoTransactionMessage message = new BackIsoTransactionMessage(
                    mti.trim(),
                    pan.trim(),
                    processingCode.trim(),
                    amount,
                    transmissionDateTime,
                    stan.trim(),
                    localTime.trim(),
                    localDate.trim(),
                    merchantType.trim(),
                    acquiringInstitutionCode.trim(),
                    retrievalReferenceNumber.trim(),
                    authorizationIdResponse.trim(),
                    responseCode.trim(),
                    terminalId.trim(),
                    merchantId.trim(),
                    currencyCode.trim(),
                    additionalData
            );

            log.debug("Parsed ISO 8583 message: {}", message);
            return message;

        } catch (Exception e) {
            log.error("Error parsing ISO 8583 message", e);
            throw new RuntimeException("Failed to parse ISO 8583 message: " + e.getMessage(), e);
        }
    }

    private String extractField(String message, int position, int length) {
        if (position + length > message.length()) {
            throw new IllegalArgumentException(
                    String.format("Message too short. Expected at least %d characters, got %d",
                            position + length, message.length()));
        }
        return message.substring(position, position + length);
    }

    private BigDecimal parseAmount(String amountStr) {
        try {
            // Amount is in cents, last 2 digits are decimal places
            long cents = Long.parseLong(amountStr.trim());
            return BigDecimal.valueOf(cents, 2);
        } catch (NumberFormatException e) {
            log.warn("Invalid amount format: {}, defaulting to 0", amountStr);
            return BigDecimal.ZERO;
        }
    }

    private LocalDateTime parseTransmissionDateTime(String dateTimeStr) {
        try {
            // Format: MMddHHmmss
            int year = LocalDateTime.now().getYear();
            String fullDateTime = String.format("%02d%02d%s",
                    year % 100,
                    Integer.parseInt(dateTimeStr.substring(0, 2)),
                    dateTimeStr.substring(2));

            // Parse as MMddHHmmss and add current year
            int month = Integer.parseInt(dateTimeStr.substring(0, 2));
            int day = Integer.parseInt(dateTimeStr.substring(2, 4));
            int hour = Integer.parseInt(dateTimeStr.substring(4, 6));
            int minute = Integer.parseInt(dateTimeStr.substring(6, 8));
            int second = Integer.parseInt(dateTimeStr.substring(8, 10));

            return LocalDateTime.of(year, month, day, hour, minute, second);
        } catch (Exception e) {
            log.warn("Invalid transmission date/time format: {}, using current time", dateTimeStr);
            return LocalDateTime.now();
        }
    }
}
