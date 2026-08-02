package com.vseonlineshop.payment.strategy;

import com.stripe.StripeClient;
import com.stripe.exception.ApiConnectionException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.vseonlineshop.payment.dto.*;
import com.vseonlineshop.payment.dto.enums.PaymentMethod;
import com.vseonlineshop.payment.exception.PaymentServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
    public PaymentProviderResponse charge(PaymentRequest orderRequest, UUID paymentId) {
        log.info("[STRIPE/START] Initialisiere Checkout-Session für Order-ID={} via {}",
                orderRequest.orderId(), orderRequest.paymentMethod());

        try {
            SessionCreateParams.Builder sessionBuilder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successPageUrl)
                    .setPaymentIntentData(
                            SessionCreateParams.PaymentIntentData.builder()
                                    .putMetadata("paymentId", paymentId.toString())
                                    .putMetadata("orderId", orderRequest.orderId().toString())
                                    .putMetadata("correlationId", orderRequest.correlationId().toString())
                                    .build()
                    );

            mapPaymentMethod(sessionBuilder, orderRequest.paymentMethod());

            long totalAmountInCent = 0L;
            String selectedCurrency = orderRequest.currency().toLowerCase();

            for (CartItemDTO item : orderRequest.cartItems()) {
                long lineTotalInCent = item.getTotalPrice()
                        .multiply(BigDecimal.valueOf(100))
                        .longValue();

                long unitAmountInCent = lineTotalInCent / item.getQuantity();

                sessionBuilder.addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(Long.valueOf(item.getQuantity()))
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(selectedCurrency)
                                .setUnitAmount(unitAmountInCent)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName(item.getProductName())
                                        .build())
                                .build())
                        .build());

                totalAmountInCent += lineTotalInCent;
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

            return new PaymentProviderResponse(
                    session.getUrl(),
                    session.getId(),
                    session.toJson()
            );

        } catch (InvalidRequestException e) {
            log.warn("[STRIPE/INVALID_REQ] Ungültige Parameter für Order-ID={}. Grund: {}",
                    orderRequest.orderId(), e.getMessage());
            throw new PaymentServiceException("Ungültige Zahlungsanforderung: " + e.getMessage(), e);

        } catch (ApiConnectionException e) {
            log.error("[STRIPE/TIMEOUT] Keine Verbindung zu Stripe für Order-ID={}",
                    orderRequest.orderId(), e);
            throw new PaymentServiceException("Netzwerkfehler bei Verbindung zu Stripe", e);

        } catch (StripeException e) {
            log.error("[STRIPE/ERROR] Fehler bei Stripe Session-Erstellung für Order-ID={}. Code={}",
                    orderRequest.orderId(), e.getCode(), e);
            throw new PaymentServiceException("Zahlung über Stripe fehlgeschlagen: " + e.getMessage(), e);

        } catch (Exception e) {
            log.error("[STRIPE/UNEXPECTED_ERROR] Unerwarteter Systemfehler bei Stripe Session-Erstellung für Order-ID={}. Grund: {}",
                    orderRequest.orderId(), e.getMessage(), e);
            throw new PaymentServiceException("Unerwarteter Fehler bei der Zahlungsabwicklung: " + e.getMessage(), e);
        }
    }

    @Override
    public String refund(String transactionId, BigDecimal amount, String currency) {
        log.info("[STRIPE/REFUND] Starte Refund für Transaktion={} mit Betrag={} {}",
                transactionId, amount, currency);

        try {
            long amountInCent = amount.multiply(new BigDecimal(100)).longValue();

            RefundCreateParams.Builder refundBuilder = RefundCreateParams.builder()
                    .setAmount(amountInCent);

            if (transactionId.startsWith("cs_")) {
                Session session = stripeClient.v1().checkout().sessions().retrieve(transactionId);
                refundBuilder.setPaymentIntent(session.getPaymentIntent());
            } else {
                refundBuilder.setPaymentIntent(transactionId);
            }

            RefundCreateParams params = refundBuilder.build();

            Refund refund = stripeClient.v1().refunds().create(params);

            log.info("[STRIPE/REFUND_SUCCESS] Refund erfolgreich ausgeführt. Stripe-Refund-ID={}, Status={}",
                    refund.getId(), refund.getStatus());

            return refund.getId();

        } catch (InvalidRequestException e) {
            log.warn("[STRIPE/REFUND_DECLINED] Refund nicht möglich für Transaktion={}. Grund: {}",
                    transactionId, e.getMessage());
            throw new PaymentServiceException("Rückerstattung nicht möglich: " + e.getMessage(), e);

        } catch (ApiConnectionException e) {
            log.error("[STRIPE/REFUND_TIMEOUT] Verbindungsfehler beim Stripe-Refund für Transaktion={}",
                    transactionId, e);
            throw new PaymentServiceException("Netzwerkfehler beim Ausführen der Rückerstattung", e);

        } catch (StripeException e) {
            log.error("[STRIPE/REFUND_ERROR] Fehler beim Stripe-Refund für Transaktion={}. Code={}",
                    transactionId, e.getCode(), e);
            throw new PaymentServiceException("Rückerstattung über Stripe fehlgeschlagen", e);
        } catch (Exception e) {
            throw new PaymentServiceException("Unerwarteter Fehler bei der Zahlungsabwicklung: " + e.getMessage(), e);
        }
    }

    @Override
    public String getStatus(String providerTransactionId) {
        try {
            Session session = stripeClient.v1()
                    .checkout()
                    .sessions()
                    .retrieve(providerTransactionId);

            return session.getPaymentStatus();
        } catch (InvalidRequestException e) {
            log.warn("[STRIPE/STATUS_NOT_FOUND] Keine Session gefunden für ID={}. Grund: {}",
                    providerTransactionId, e.getMessage());
            return "UNKNOWN";

        } catch (ApiConnectionException e) {
            log.error("[STRIPE/STATUS_TIMEOUT] Verbindungsfehler beim Statusabruf für ID={}",
                    providerTransactionId, e);
            return "UNKNOWN";

        } catch (StripeException e) {
            log.error("[STRIPE/STATUS_ERROR] Fehler beim Abrufen des Status für ID={}. Code={}",
                    providerTransactionId, e.getCode(), e);
            return "UNKNOWN";
        } catch (Exception e) {
            throw new PaymentServiceException("Unerwarteter Fehler bei der Zahlungsabwicklung: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean supportsTransactionId(String transactionId) {
        return transactionId != null && (
                transactionId.startsWith("cs_") ||
                        transactionId.startsWith("pi_") ||
                        transactionId.startsWith("ch_")
        );
    }

    @Override
    public String getProviderName() {
        return "STRIPE";
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


