package com.vseonlineshop.payment.strategy;

import com.vseonlineshop.payment.dto.enums.PaymentMethod;
import com.vseonlineshop.payment.dto.PaymentRequest;
import com.vseonlineshop.payment.dto.PaymentProviderResponse;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentStrategy {


    String getProviderName();

    boolean supports(PaymentMethod method);

    PaymentProviderResponse charge(PaymentRequest request, UUID paymentId);

    String refund(String transactionId, BigDecimal amount, String currency);

    String getStatus(String transactionId);

    boolean supportsTransactionId(String transactionId);
}
