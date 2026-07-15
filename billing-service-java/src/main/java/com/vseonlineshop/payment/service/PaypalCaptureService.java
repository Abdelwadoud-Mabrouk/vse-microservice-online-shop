package com.vseonlineshop.payment.service;

import com.paypal.sdk.PaypalServerSdkClient;
import com.paypal.sdk.controllers.OrdersController;
import com.paypal.sdk.http.response.ApiResponse;
import com.paypal.sdk.models.CaptureOrderInput;
import com.paypal.sdk.models.Order;
import com.vseonlineshop.payment.exception.GlobalException;
import com.vseonlineshop.payment.exception.PaypalCaptureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaypalCaptureService {

    private final PaypalServerSdkClient paypalClient;
    private final KafkaEventPublisher kafkaEventPublisher;

    public String captureOrder(String orderId) {

        log.info("[PAYPAL/CAPTURE] Starte Geld-Einzug für Order-ID: {}", orderId);

        try {
            OrdersController ordersController = paypalClient.getOrdersController();

            CaptureOrderInput captureOrderInput = new CaptureOrderInput.Builder(
                    orderId,
                    null
            )
                    .prefer("return=representation")
                    .build();

            ApiResponse<Order> response = ordersController.captureOrder(captureOrderInput);
            Order capturedOrder = response.getResult();

            log.info("[PAYPAL/CAPTURE] Antwort erhalten. Status: {}", capturedOrder.getStatus());

            if ("COMPLETED".equalsIgnoreCase(capturedOrder.getStatus().toString())) {
                log.info("[PAYPAL/SUCCESS] Geld eingezogen! Webhook wird  gefeuert...");
                return "Zahlung erfolgreich gecaptured!";
            } else {
                publishSystemAlert(orderId);
                throw new PaypalCaptureException("Zahlung fehlgeschlagen. Status: " + capturedOrder.getStatus());
            }

        } catch (Exception e) {
            log.error("[PAYPAL/CAPTURE-ERROR] Fehler beim Einziehen des Geldes", e);
            publishSystemAlert(orderId);
            throw new GlobalException("Fehler beim Zahlungseinzug");
        }
    }

    private void publishSystemAlert(String orderId) {
        safePublish(() -> kafkaEventPublisher.publishSystemAlert(orderId, " [PAYPAL/CAPTURE-ERROR] Fehler beim Einziehen des Geldes "));
    }

    private void safePublish(Runnable fn) {
        try {
            fn.run();
        } catch (Exception ex) {
            log.error("[Kafka] safePublish fehlgeschlagen: {}", ex.getMessage(), ex);
        }
    }
}
