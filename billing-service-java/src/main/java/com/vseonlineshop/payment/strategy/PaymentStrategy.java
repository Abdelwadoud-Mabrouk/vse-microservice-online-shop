package com.vseonlineshop.payment.strategy;

import com.vseonlineshop.payment.dto.PaymentRequest;
import com.vseonlineshop.payment.dto.ProviderResponse;
import com.vseonlineshop.payment.enums.PaymentProvider;

public interface PaymentStrategy {
    boolean supports(PaymentProvider provider);
    ProviderResponse processPayment(PaymentRequest request);
}
