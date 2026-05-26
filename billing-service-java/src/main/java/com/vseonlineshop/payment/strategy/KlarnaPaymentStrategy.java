package com.vseonlineshop.payment.strategy;

import com.vseonlineshop.payment.dto.PaymentRequest;
import com.vseonlineshop.payment.dto.ProviderResponse;
import com.vseonlineshop.payment.enums.PaymentProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Slf4j
@Component
public class KlarnaPaymentStrategy implements PaymentStrategy {

    @Override
    public boolean supports(PaymentProvider provider) {
        return provider == PaymentProvider.KLARNA;
    }

    @Override
    public ProviderResponse processPayment(PaymentRequest request) {
        log.info("[KLARNA] Processing payment for order={}", request.orderId());

        // TODO: echter Klarna SDK-Call
        String fakeTransactionId = "KLARNA-TXN-" + UUID.randomUUID();
        String fakeRaw = """
            {"order_id":"%s","status":"AUTHORIZED"}
            """.formatted(fakeTransactionId);

        return null;
    }
}
