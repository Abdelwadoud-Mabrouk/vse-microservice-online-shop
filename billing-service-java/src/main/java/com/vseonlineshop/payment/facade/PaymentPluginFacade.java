package com.vseonlineshop.payment.facade;

import com.vseonlineshop.payment.dto.PaymentRequest;
import com.vseonlineshop.payment.dto.ProviderResponse;
import com.vseonlineshop.payment.enums.PaymentProvider;
import com.vseonlineshop.payment.strategy.PaymentStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class PaymentPluginFacade {

    // Spring injiziert automatisch ALLE PaymentStrategy-Implementierungen
    private final List<PaymentStrategy> strategies;

    public ProviderResponse execute(PaymentRequest request) {
        return strategies.stream()
            .filter(s -> s.supports(request.paymentProvider()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Provider nicht unterstützt: " + request.paymentProvider()))
            .processPayment(request);
    }

    // Nützlich für Health-Check: welche Provider sind aktiv?
    public List<PaymentProvider> supportedProviders() {
        return Stream.of(PaymentProvider.values())
            .filter(p -> strategies.stream().anyMatch(s -> s.supports(p)))
            .toList();
    }
}
