package com.vseonlineshop.payment.strategy;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.vseonlineshop.payment.dto.*;
import com.vseonlineshop.payment.exception.PaymentServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class StripePaymentStrategy implements PaymentStrategy {


    private final StripeClient stripeClient;
    private final String successPageUrl;

    public StripePaymentStrategy(
            @Value("${stripe.secret.key}") String stripeSecretKey,
            @Value("${success.page}") String successPageUrl) {

        this.successPageUrl = successPageUrl;
        this.stripeClient = new StripeClient(stripeSecretKey);
    }


    @Override
    public boolean supports(PaymentMethod method) {

        return method == PaymentMethod.CREDIT_CARD || method == PaymentMethod.KLARNA
                || method == PaymentMethod.APPLE_PAY || method == PaymentMethod.GOOGLE_PAY;
    }


    @Override
    public SmallProviderResponse send(PaymentRequest orderRequest, UUID paymentId) {
        log.info("[STRIPE/START] Initialisiere Checkout-Session für Order-ID={} via {}",
                orderRequest.orderId(), orderRequest.paymentMethod());

        try {
            SessionCreateParams.Builder sessionBuilder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                   // .setSuccessUrl("http://127.0.0.1:5500/success.html")
                    .setSuccessUrl(successPageUrl)
                    .setPaymentIntentData(
                            SessionCreateParams.PaymentIntentData.builder()
                                    .putMetadata("paymentId",     paymentId.toString())
                                    .putMetadata("orderId",       orderRequest.orderId().toString())
                                    .putMetadata("correlationId", orderRequest.correlationId().toString())
                                    .build()
                    );

            mapPaymentMethod(sessionBuilder, orderRequest.paymentMethod());

            long totalAmountInCent = 0L;
            String selectedCurrency = orderRequest.currency().toLowerCase();

            for (OrderItem item : orderRequest.cartItems()) {
                long unitAmountInCent = item.getPricePerUnit()
                        .multiply(new java.math.BigDecimal(100))
                        .longValue();

                sessionBuilder.addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity((long) item.getQuantity())
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(selectedCurrency)
                                .setUnitAmount(unitAmountInCent)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName(item.getProductName())
                                        .build())
                                .build())
                        .build());

                totalAmountInCent += unitAmountInCent * item.getQuantity();
            }

            log.info("[STRIPE/PROCESS] Parameter kontrolliert: Order-ID={}, Währung={}, Gesamtsumme(Cent)={}",
                    orderRequest.orderId(), selectedCurrency.toUpperCase(), totalAmountInCent);

            SessionCreateParams params = sessionBuilder.build();
            Session session = stripeClient.v1()
                    .checkout()
                    .sessions()
                    .create(params);

            log.info("[STRIPE/SUCCESS] Checkout-Session erfolgreich erstellt. Stripe-ID={}, URL={}",
                    session.getId(), session.getUrl());

            return new SmallProviderResponse(
                    session.getUrl(),
                    session.getId(),
                    session.toJson()
            );

        } catch (StripeException e) {
            log.error("[STRIPE/ERROR] Fehler bei der Stripe Session-Erstellung für Order-ID={}. Grund: {}",
                    orderRequest.orderId(), e.getMessage(), e);
            throw new PaymentServiceException("Zahlung über Stripe fehlgeschlagen");
        }
    }

    private void mapPaymentMethod(SessionCreateParams.Builder builder, PaymentMethod method) {

        switch (method) {
            case KLARNA -> builder.addPaymentMethodType(SessionCreateParams.PaymentMethodType.KLARNA);
            case CREDIT_CARD -> builder.addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD);
            default -> {
            }
        }
    }
}


