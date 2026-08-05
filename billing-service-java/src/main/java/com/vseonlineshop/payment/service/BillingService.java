package com.vseonlineshop.payment.service;

import com.vseonlineshop.payment.dto.*;
import com.vseonlineshop.payment.dto.refund.RefundProviderResponse;
import com.vseonlineshop.payment.dto.refund.RefundRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;


import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor

public class BillingService {

    private final PaymentProcessService paymentProcessService;


    public PaymentResponse initiatePayment(PaymentRequest request, UUID correlationId) {

        MDC.put(
                "correlationId",
                correlationId.toString()
        );

        try {
            PaymentRequest enrichedRequest =
                    request.withCorrelationId(correlationId);


            return paymentProcessService.processPayment(enrichedRequest);

        } finally {
            MDC.clear();
        }
    }

    public RefundProviderResponse processRefund(RefundRequestDto refundRequestDto, UUID correlationId) {
        MDC.put(
                "correlationId",
                correlationId.toString()
        );

        try {
            return paymentProcessService.processRefund(
                    refundRequestDto,
                    correlationId
            );

        } finally {
            MDC.clear();
        }

    }

    public PaymentResponse getByCorrelationId(UUID correlationId) {
        MDC.put(
                "correlationId",
                correlationId.toString()
        );

        try {
            return paymentProcessService.getByCorrelationId(correlationId);

        } finally {
            MDC.clear();
        }
    }

    public PaymentStatusResponse getStatus(String transactionId, UUID correlationId) {
        MDC.put(
                "correlationId",
                correlationId.toString()
        );

        try {

            return paymentProcessService.getStatus(transactionId);

        } finally {
            MDC.clear();
        }
    }
}
