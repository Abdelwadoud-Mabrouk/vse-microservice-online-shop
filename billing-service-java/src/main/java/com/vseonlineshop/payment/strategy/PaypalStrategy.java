package com.vseonlineshop.payment.strategy;

import com.paypal.sdk.controllers.OrdersController;
import com.paypal.sdk.http.response.ApiResponse;
import com.paypal.sdk.models.*;
import com.vseonlineshop.payment.strategy.config.PaypalConfig;
import com.vseonlineshop.payment.dto.OrderItem;
import com.vseonlineshop.payment.dto.PaymentMethod;
import com.vseonlineshop.payment.dto.PaymentRequest;
import com.vseonlineshop.payment.dto.SmallProviderResponse;
import com.vseonlineshop.payment.exception.PaymentServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaypalStrategy implements PaymentStrategy {


    private final PaypalConfig paypalConfig;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(PaymentMethod method) {
        return PaymentMethod.PAYPAL.equals(method);
    }

    @Override
    public SmallProviderResponse send(PaymentRequest orderRequest, UUID paymentId) {
        log.info("[PAYPAL/START] Initialisiere Order für Order-ID={}", orderRequest.orderId());
        try {

            // 2. Items aufbauen (analog zu deinen LineItems bei Stripe)
            List<ItemRequest> items = new ArrayList<>();
            BigDecimal totalAmount = BigDecimal.ZERO;
            String currency = orderRequest.currency().toUpperCase();

            for (OrderItem cartItem : orderRequest.cartItems()) {

                String unitPriceFormatted = String.format(java.util.Locale.US, "%.2f", cartItem.getPricePerUnit());
                items.add(new ItemRequest.Builder(
                                cartItem.getProductName(),
                                new Money.Builder(currency, unitPriceFormatted).build(),
                                String.valueOf(cartItem.getQuantity())

                        ).build()
                );
                totalAmount = totalAmount.add(
                        cartItem.getPricePerUnit()
                                .multiply(new BigDecimal(cartItem.getQuantity()))
                );
            }

            // 3. PurchaseUnit mit Metadata (entspricht putMetadata bei Stripe)
            String customIdPayload = String.format("paymentId=%s|orderId=%s|correlationId=%s",
                    paymentId,
                    orderRequest.orderId().toString(),
                    orderRequest.correlationId().toString()
            );

            String totalAmountFormatted = String.format(java.util.Locale.US, "%.2f", totalAmount);
            PurchaseUnitRequest purchaseUnit = new PurchaseUnitRequest.Builder(
                    new AmountWithBreakdown.Builder(currency, totalAmountFormatted)
                            .breakdown(new AmountBreakdown.Builder()
                                    .itemTotal(new Money.Builder(currency, totalAmount.toString()).build())
                                    .build())
                            .build()
            )
                    .customId(customIdPayload)
                    .items(items)
                    .build();

            // 4. Order Request (analog zu SessionCreateParams bei Stripe)
            OrderRequest orderPayload = new OrderRequest.Builder(
                    CheckoutPaymentIntent.CAPTURE,
                    List.of(purchaseUnit)
            )
                    .paymentSource(new PaymentSource.Builder()
                            .paypal(new PaypalWallet.Builder()
                                    .experienceContext(new PaypalWalletExperienceContext.Builder()
                                            .returnUrl("http://127.0.0.1:5500/success.html")
                                            .cancelUrl("http://127.0.0.1:5500/cancel.html")
                                            .userAction(PaypalExperienceUserAction.PAY_NOW)
                                            .build())
                                    .build())
                            .build())
                    .build();

            // 5. API-Call
            OrdersController ordersController = paypalConfig.paypalClient().getOrdersController();
            CreateOrderInput createOrderInput = new CreateOrderInput();
            createOrderInput.setBody(orderPayload);
            ApiResponse<Order> response = ordersController.createOrder(createOrderInput);
            Order paypalOrder = response.getResult();

            // 6. Approve-URL aus HATEOAS-Links extrahieren (das ist die redirect-URL)
            String approveUrl = paypalOrder.getLinks().stream()
                    .filter(link -> "payer-action".equals(link.getRel()))
                    .findFirst()
                    .map(LinkDescription::getHref)
                    .orElseThrow(() -> new PaymentServiceException("Keine Approve-URL von PayPal erhalten"));

            log.info("[PAYPAL/SUCCESS] Order erstellt. PayPal-ID={}, URL={}", paypalOrder.getId(), approveUrl);

            String auditJson;
            try {
                // Wandelt das Java-Objekt "paypalOrder" in einen JSON-String um
                auditJson = objectMapper.writeValueAsString(paypalOrder);
            } catch (Exception e) {
                log.warn("[PAYPAL/AUDIT] Konnte Order-Objekt nicht in JSON umwandeln, nutze Fallback-String", e);
                auditJson = paypalOrder.toString(); // Fallback, damit die Methode nicht komplett abstürzt
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
}
