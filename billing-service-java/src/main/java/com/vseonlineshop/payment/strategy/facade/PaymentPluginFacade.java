package com.vseonlineshop.payment.strategy.facade;

import com.vseonlineshop.payment.dto.PaymentStatusResponse;
import com.vseonlineshop.payment.dto.refund.RefundProviderResponse;
import com.vseonlineshop.payment.dto.enums.PaymentMethod;
import com.vseonlineshop.payment.dto.PaymentRequest;
import com.vseonlineshop.payment.dto.PaymentProviderResponse;
import com.vseonlineshop.payment.exception.PaymentServiceException;
import com.vseonlineshop.payment.strategy.PaymentStrategy;
import com.vseonlineshop.payment.strategy.config.PaymentRoutingProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class PaymentPluginFacade {

    private final List<PaymentStrategy> providers;
    private final PaymentRoutingProperties routingProperties;

    public PaymentPluginFacade(List<PaymentStrategy> providers,
                               PaymentRoutingProperties routingProperties) {
        this.providers = providers;
        this.routingProperties = routingProperties;
    }

    public PaymentProviderResponse charge(PaymentRequest request, UUID paymentId) throws PaymentServiceException {
        PaymentStrategy strategy = resolveStrategy(request.paymentMethod());
        log.info("[PaymentFacade] Nutze Provider '{}' für Methode '{}'",
                strategy.getProviderName(), request.paymentMethod());

        return strategy.charge(request, paymentId);
    }

    public RefundProviderResponse refund(String transactionId, BigDecimal amount, String currency, PaymentMethod paymentMethod) {

        PaymentStrategy strategy = resolveStrategy(paymentMethod);
        log.info("[PaymentFacade] Führe Refund über Provider '{}' aus für Transaktion '{}'",
                strategy.getProviderName(), transactionId);
        return strategy.refund(transactionId, amount, currency);
    }


    public PaymentStatusResponse getStatus(String transactionId) {
        PaymentStrategy strategy = providers.stream()
                .filter(s -> s.supportsTransactionId(transactionId))
                .findFirst()
                .orElseThrow(() -> new PaymentServiceException("Kein Provider für Transaktions-ID '" + transactionId + "' gefunden"));

        log.info("[PaymentFacade] Rufe Status für Transaktion '{}' über Provider '{}' ab",
                transactionId, strategy.getProviderName());

        return strategy.getStatus(transactionId);
    }

    private PaymentStrategy resolveStrategy(PaymentMethod method) {
        String targetProvider = routingProperties.getRouting().get(method);

        if (targetProvider == null) {
            throw new PaymentServiceException("Kein Provider-Routing für Zahlungsmethode '" + method + "' in der Konfiguration definiert");
        }

        return providers.stream()
                .filter(p -> p.getProviderName().equalsIgnoreCase(targetProvider))
                .filter(p -> p.supports(method))
                .findFirst()
                .orElseThrow(() -> new PaymentServiceException(
                        String.format("Konfigurierter Provider '%s' unterstützt Zahlungsmethode '%s' nicht oder ist nicht geladen",
                                targetProvider, method)
                ));
    }
}