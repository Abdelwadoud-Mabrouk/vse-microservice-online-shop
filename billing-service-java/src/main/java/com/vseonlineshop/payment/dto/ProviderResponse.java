package com.vseonlineshop.payment.dto;

public record ProviderResponse(
        boolean success,
        String providerTransactionId,
        String approvalUrl,
        String rawResponse,
        String failureReason
) {
}
