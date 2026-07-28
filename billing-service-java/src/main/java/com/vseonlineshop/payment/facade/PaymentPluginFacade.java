package com.vseonlineshop.payment.facade;

import com.vseonlineshop.payment.dto.enums.PaymentMethod;
import com.vseonlineshop.payment.dto.PaymentRequest;
import com.vseonlineshop.payment.dto.PaymentProviderResponse;
import com.vseonlineshop.payment.exception.PaymentServiceException;
import com.vseonlineshop.payment.strategy.PaymentStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class PaymentPluginFacade {

    private final List<PaymentStrategy> providers;

    @Value("${payment.provider.active:AUTO}")
    private String activeProvider;

    public PaymentPluginFacade(List<PaymentStrategy> providers) {
        this.providers = providers;
    }

    public PaymentProviderResponse charge(PaymentRequest request, UUID paymentId) {
        PaymentStrategy strategy = resolveStrategy(request.paymentMethod());
        log.info("[PaymentFacade] Nutze Provider '{}' für Methode '{}'",
                strategy.getProviderName(), request.paymentMethod());

        return strategy.charge(request, paymentId);
    }

    public String refund(String transactionId, BigDecimal amount, String currency, PaymentMethod paymentMethod) {

        PaymentStrategy strategy = providers.stream()
                .filter(s -> s.supports(paymentMethod))
                .findFirst()
                .orElseThrow(() -> new PaymentServiceException("Keine passende PaymentStrategy für " + paymentMethod + " gefunden"));

        return strategy.refund(transactionId, amount, currency);
    }


    public String getStatus(String transactionId) {

        PaymentStrategy strategy = providers.stream()
                .filter(s -> s.supportsTransactionId(transactionId))
                .findFirst()
                .orElseGet(this::getPreferredStrategy);

        log.info("[PaymentFacade] Rufe Status für Transaktion '{}' über Provider '{}' ab",
                transactionId, strategy.getProviderName());

        return strategy.getStatus(transactionId);
    }

    private PaymentStrategy resolveStrategy(PaymentMethod method) {
        if (!"AUTO".equalsIgnoreCase(activeProvider)) {
            var configuredStrategy = providers.stream()
                    .filter(p -> p.getProviderName().equalsIgnoreCase(activeProvider))
                    .filter(p -> p.supports(method))
                    .findFirst();

            if (configuredStrategy.isPresent()) {
                return configuredStrategy.get();
            }

            log.warn("[PaymentFacade] Konfigurierter Provider '{}' unterstützt '{}' nicht. Weiche auf passenden Provider aus.",
                    activeProvider, method);
        }


        return providers.stream()
                .filter(p -> p.supports(method))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Kein Provider für Zahlungsmethode gefunden: " + method));
    }

    private PaymentStrategy getPreferredStrategy() {
        return providers.stream()
                .filter(p -> p.getProviderName().equalsIgnoreCase(activeProvider))
                .findFirst()
                .orElse(providers.get(0)); // Fallback auf den ersten in der Liste
    }
}