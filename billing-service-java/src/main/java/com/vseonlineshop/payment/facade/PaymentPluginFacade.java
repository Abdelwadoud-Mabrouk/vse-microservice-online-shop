package com.vseonlineshop.payment.facade;

import com.vseonlineshop.payment.dto.PaymentRequest;
import com.vseonlineshop.payment.dto.SmallProviderResponse;
import com.vseonlineshop.payment.strategy.PaymentStrategy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class PaymentPluginFacade {

    private final List<PaymentStrategy> providers;

    public PaymentPluginFacade(List<PaymentStrategy> providers) {
        this.providers = providers;
    }

    public SmallProviderResponse execute(PaymentRequest request, UUID paymentId) {
        return providers.stream()
                .filter(p -> p.supports(request.paymentMethod()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Zahlungsmethode nicht unterstützt: " + request.paymentMethod()))
                .send(request,paymentId);
    }
}