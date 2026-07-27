package com.vseonlineshop.payment.strategy;

import com.paypal.sdk.controllers.OrdersController;
import com.paypal.sdk.controllers.PaymentsController;
import com.paypal.sdk.http.response.ApiResponse;
import com.paypal.sdk.models.*;
import com.vseonlineshop.payment.entity.PaymentTransaction;
import com.vseonlineshop.payment.repository.PaymentTransactionRepository;
import com.vseonlineshop.payment.strategy.config.PaypalConfig;
import com.vseonlineshop.payment.dto.CartItemDTO;
import com.vseonlineshop.payment.dto.enums.PaymentMethod;
import com.vseonlineshop.payment.dto.PaymentRequest;
import com.vseonlineshop.payment.dto.SmallProviderResponse;
import com.vseonlineshop.payment.exception.PaymentServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaypalStrategy implements PaymentStrategy {


    private final PaypalConfig paypalConfig;
    private final ObjectMapper objectMapper;
    private final PaymentTransactionRepository paymentTransactionRepository;
    @Value("${success.page}")
    private String successPageUrl;

    @Override
    public boolean supports(PaymentMethod method) {
        return PaymentMethod.PAYPAL.equals(method);
    }

    @Override
    public SmallProviderResponse charge(PaymentRequest orderRequest, UUID paymentId) {
        log.info("[PAYPAL/START] Initialisiere Order für Order-ID={}", orderRequest.orderId());
        try {

            List<ItemRequest> items = new ArrayList<>();
            BigDecimal totalAmount = BigDecimal.ZERO;
            String currency = orderRequest.currency().toUpperCase();

            for (CartItemDTO cartItem : orderRequest.cartItems()) {

                // 1. Brutto-Einzelpreis berechnen: (TotalPrice inkl. MwSt) / Menge
                BigDecimal unitPriceWithTax = cartItem.getTotalPrice()
                        .divide(BigDecimal.valueOf(cartItem.getQuantity()), 2, RoundingMode.HALF_UP);

                String unitPriceFormatted = String.format(java.util.Locale.US, "%.2f", unitPriceWithTax);

                // 2. Item für PayPal aufbauen
                items.add(new ItemRequest.Builder(
                                cartItem.getProductName(),
                                new Money.Builder(currency, unitPriceFormatted).build(),
                                String.valueOf(cartItem.getQuantity())
                        ).build()
                );

                // 3. Position zur Gesamtsumme addieren (Brutto-Gesamtpreis der Zeile)
                totalAmount = totalAmount.add(cartItem.getTotalPrice());
            }

            // 4. Beträge für PayPal-API formatieren ("119.00")
            String totalAmountFormatted = String.format(java.util.Locale.US, "%.2f", totalAmount);

            // 5. Custom ID mit Metadata
            String customIdPayload = String.format("paymentId=%s|orderId=%s|correlationId=%s",
                    paymentId,
                    orderRequest.orderId().toString(),
                    orderRequest.correlationId().toString()
            );

            // 6. PurchaseUnit bauen
            PurchaseUnitRequest purchaseUnit = new PurchaseUnitRequest.Builder(
                    new AmountWithBreakdown.Builder(currency, totalAmountFormatted)
                            .breakdown(new AmountBreakdown.Builder()
                                    .itemTotal(new Money.Builder(currency, totalAmountFormatted).build())
                                    .build())
                            .build()
            )
                    .customId(customIdPayload)
                    .items(items)
                    .build();

            // 7. Order Request aufbauen
            OrderRequest orderPayload = new OrderRequest.Builder(
                    CheckoutPaymentIntent.CAPTURE,
                    List.of(purchaseUnit)
            )
                    .paymentSource(new PaymentSource.Builder()
                            .paypal(new PaypalWallet.Builder()
                                    .experienceContext(new PaypalWalletExperienceContext.Builder()
                                            .returnUrl(successPageUrl)
                                            .cancelUrl("http://127.0.0.1:5500/cancel.html")
                                            .userAction(PaypalExperienceUserAction.PAY_NOW)
                                            .build())
                                    .build())
                            .build())
                    .build();

            // 8. API-Call an PayPal
            OrdersController ordersController = paypalConfig.paypalClient().getOrdersController();
            CreateOrderInput createOrderInput = new CreateOrderInput();
            createOrderInput.setBody(orderPayload);
            ApiResponse<Order> response = ordersController.createOrder(createOrderInput);
            Order paypalOrder = response.getResult();

            // 9. Approve-URL extrahieren
            String approveUrl = paypalOrder.getLinks().stream()
                    .filter(link -> "payer-action".equals(link.getRel()))
                    .findFirst()
                    .map(LinkDescription::getHref)
                    .orElseThrow(() -> new PaymentServiceException("Keine Approve-URL von PayPal erhalten"));

            log.info("[PAYPAL/SUCCESS] Order erstellt. PayPal-ID={}, URL={}", paypalOrder.getId(), approveUrl);

            String auditJson;
            try {
                auditJson = objectMapper.writeValueAsString(paypalOrder);
            } catch (Exception e) {
                log.warn("[PAYPAL/AUDIT] Konnte Order-Objekt nicht in JSON umwandeln", e);
                auditJson = paypalOrder.toString();
            }

            return new SmallProviderResponse(
                    approveUrl,
                    paypalOrder.getId(),
                    auditJson
            );

        } catch (Exception e) {
            log.error("[PAYPAL/ERROR] Fehler für Order-ID={}. Grund: {}",
                    orderRequest.orderId(), e.getMessage(), e);
            throw new PaymentServiceException("Zahlung über PayPal fehlgeschlagen");
        }
    }

    @Override
    public String refund(String transactionId, BigDecimal amount, String currency) {
        log.info("[PAYPAL/REFUND] Starte Refund für Transaktion={} mit Betrag={} {}",
                transactionId, amount, currency);

        try {
            // 1. Payment aus der Datenbank laden
            PaymentTransaction payment = paymentTransactionRepository.findPaymentTransactionsByProviderTransactionIdAndCaptureIdNotNull(transactionId)
                    .orElseThrow(() -> new PaymentServiceException(
                            "Keine Zahlung für Transaktions-ID " + transactionId + " gefunden."));

            // 2. Capture-ID prüfen (sollte im Webhook befüllt worden sein)
            String captureId = payment.getCaptureId();
            log.info("[PAYPAL/REFUND] Verwende Capture-ID={} für den Refund-Call", captureId);

            // 3. Betrag formatieren & Request Payload aufbauen
            String amountFormatted = String.format(java.util.Locale.US, "%.2f", amount);
            String currencyUpper = currency.toUpperCase();

            Money money = new Money.Builder(currencyUpper, amountFormatted).build();
            RefundRequest refundRequest = new RefundRequest.Builder()
                    .amount(money)
                    .noteToPayer("Rückerstattung für Bestellung")
                    .build();

            // 4. Refund über PayPal PaymentsController ausführen
            RefundCapturedPaymentInput input = new RefundCapturedPaymentInput();
            input.setCaptureId(captureId);
            input.setBody(refundRequest);

            PaymentsController paymentsController = paypalConfig.paypalClient().getPaymentsController();
            ApiResponse<Refund> response = paymentsController.refundCapturedPayment(input);
            Refund refund = response.getResult();

            log.info("[PAYPAL/REFUND_SUCCESS] Refund erfolgreich ausgeführt. PayPal-Refund-ID={}, Status={}",
                    refund.getId(), refund.getStatus());

            return refund.getId();

        } catch (Exception e) {
            log.error("[PAYPAL/REFUND_ERROR] Fehler beim PayPal-Refund für Transaktion={}. Grund: {}",
                    transactionId, e.getMessage(), e);
            throw new PaymentServiceException("Rückerstattung über PayPal fehlgeschlagen: " + e.getMessage());
        }
    }

    @Override
    public String getProviderName() {
        return "PAYPAL";
    }

    @Override
    public String getStatus(String providerTransactionId) {

        try {

            OrdersController ordersController =
                    paypalConfig.paypalClient()
                            .getOrdersController();


            GetOrderInput input =
                    new GetOrderInput(
                            providerTransactionId,
                            null,
                            null,   // paypalAuthAssertion
                            null    // fields
                    );


            ApiResponse<Order> response =
                    ordersController.getOrder(input);


            Order paypalOrder = response.getResult();


            return paypalOrder.getStatus() != null
                    ? paypalOrder.getStatus().name()
                    : "UNKNOWN";


        } catch (Exception e) {

            log.error(
                    "[PAYPAL] Live-Status konnte nicht abgerufen werden für ID={}",
                    providerTransactionId,
                    e
            );

            return "UNKNOWN";
        }
    }

    @Override
    public boolean supportsTransactionId(String transactionId) {
        return transactionId != null && (
                transactionId.startsWith("PAYID-") ||
                        (!transactionId.startsWith("cs_") && !transactionId.startsWith("pi_"))
        );
    }
}
