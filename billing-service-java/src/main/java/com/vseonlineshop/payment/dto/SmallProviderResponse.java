package com.vseonlineshop.payment.dto;

public record SmallProviderResponse(
        String approvalUrl,
        String providerTransactionId,
        String rawResponse
) {}