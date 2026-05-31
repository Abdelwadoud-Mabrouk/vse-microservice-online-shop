package com.vseonlineshop.payment.strategy;

import com.vseonlineshop.payment.dto.PaymentMethod;
import com.vseonlineshop.payment.dto.PaymentRequest;
import com.vseonlineshop.payment.dto.SmallProviderResponse;

import java.util.UUID;

public interface PaymentStrategy {
    boolean supports(PaymentMethod method);

    SmallProviderResponse send(PaymentRequest request, UUID paymentId);
}
