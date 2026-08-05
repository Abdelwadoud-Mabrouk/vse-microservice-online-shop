package com.vseonlineshop.payment.dto.refund;

import com.vseonlineshop.payment.entity.PaymentStatus;
import lombok.Builder;

@Builder
public record RefundProviderResponse(String refundId, PaymentStatus paymentStatus) {
}
