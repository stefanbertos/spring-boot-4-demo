package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BackIsoTransactionMessage(
        @JsonProperty("mti")
        String messageTypeIndicator,

        @JsonProperty("pan")
        String primaryAccountNumber,

        @JsonProperty("processingCode")
        String processingCode,

        @JsonProperty("transactionAmount")
        BigDecimal transactionAmount,

        @JsonProperty("transmissionDateTime")
        LocalDateTime transmissionDateTime,

        @JsonProperty("stan")
        String systemTraceAuditNumber,

        @JsonProperty("localTransactionTime")
        String localTransactionTime,

        @JsonProperty("localTransactionDate")
        String localTransactionDate,

        @JsonProperty("merchantType")
        String merchantType,

        @JsonProperty("acquiringInstitutionCode")
        String acquiringInstitutionCode,

        @JsonProperty("retrievalReferenceNumber")
        String retrievalReferenceNumber,

        @JsonProperty("authorizationIdResponse")
        String authorizationIdResponse,

        @JsonProperty("responseCode")
        String responseCode,

        @JsonProperty("terminalId")
        String terminalId,

        @JsonProperty("merchantId")
        String merchantId,

        @JsonProperty("currencyCode")
        String currencyCode,

        @JsonProperty("additionalData")
        String additionalData
) {
}
