package com.vseonlineshop.shopservice.service.client;


import com.vseonlineshop.shopservice.domain.Order;
import com.vseonlineshop.shopservice.dto.payment.*;
import com.vseonlineshop.shopservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import java.util.Map;


/**
 * Echte Implementierung: RestTemplate / WebClient auf ${payment.base-url}/payments
 * Der paymentMethod aus dem Order-Objekt wird in den Request eingebaut,
 * damit das echte Billing-Service später den richtigen Provider (PayPal/Stripe/Klarna)
 * aufrufen kann.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BillingService {


    private final OrderRepository orderRepository;
    private final BillingGateway paymentGateway;


    public PaymentResponse pay(Order order) {


        Order fullOrder =
                orderRepository.findByIdWithItems(order.getId());


        PaymentRequest request =
                new PaymentRequest(
                        fullOrder.getId(),
                        fullOrder.getUserId(),
                        fullOrder.getTotalAmount(),
                        fullOrder.getCurrency(),
                        fullOrder.getPaymentMethod(),
                        fullOrder.getItems()
                                .stream()
                                .map(item ->
                                        new CartItemDTO(
                                                item.getProductName(),
                                                item.getQuantity(),
                                                item.getProductId(),
                                                item.getTotalPrice()
                                        ))
                                .toList()
                );


        return paymentGateway.pay(
                order.getCorrelationId(),
                request,
                order.getId()
        );

    }


    public RefundResponseDto refund(Order order) {
        log.info("[BillingService] Starte Refund-Kompensation für Order={}", order.getId());

        RefundRequestDto request = new RefundRequestDto(
                order.getPaymentMethod(),
                order.getTotalAmount(),
                order.getCurrency()
        );

        return paymentGateway.refund(order.getCorrelationId(), request, order.getId());
    }

    public Map<String, String> capturePaypal(String token) {
        log.info("[PaymentClient] Sende PayPal-Capture-Request für Token={}", token);
        return paymentGateway.capturePaypal(token);
    }
}