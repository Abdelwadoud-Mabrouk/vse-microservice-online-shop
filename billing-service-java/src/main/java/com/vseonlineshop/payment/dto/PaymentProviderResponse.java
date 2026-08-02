package com.vseonlineshop.payment.dto;

public record PaymentProviderResponse(
        String approvalUrl,
        String providerTransactionId,
        String rawResponse
) {}