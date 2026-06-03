package com.vseonlineshop.shopservice.service.client;


import com.vseonlineshop.shopservice.domain.Order;
import com.vseonlineshop.shopservice.dto.payment.CartItemDTO;
import com.vseonlineshop.shopservice.dto.payment.PaymentRequest;
import com.vseonlineshop.shopservice.dto.payment.PaymentResponse;
import com.vseonlineshop.shopservice.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;


/**
 * Echte Implementierung: RestTemplate / WebClient auf ${payment.base-url}/payments
 * Der paymentMethod aus dem Order-Objekt wird in den Request eingebaut,
 * damit das echte Billing-Service später den richtigen Provider (PayPal/Stripe/Klarna)
 * aufrufen kann.
 */
@Service
@Slf4j
public class PaymentClient {


    private final RestClient restClient;
    private final OrderRepository orderRepository;

    public PaymentClient(@Value("${services.payment.url}") String paymentServiceUrl, OrderRepository orderRepository) {
        this.restClient = RestClient.builder()
                .baseUrl(paymentServiceUrl)
                .build();
        this.orderRepository = orderRepository;
    }

    public PaymentResponse pay(Order order) {
        log.info("[PaymentClient] Synchroner Call für Order={}", order.getId());

        Order fullOrder = orderRepository.findByIdWithItems(order.getId());

        PaymentRequest request = new PaymentRequest(
                fullOrder.getCorrelationId(),
                fullOrder.getId(),
                fullOrder.getUserId(),
                fullOrder.getTotalAmount(),
                fullOrder.getCurrency(),
                fullOrder.getPaymentMethod(),
                fullOrder.getItems().stream().map(orderItem -> new CartItemDTO(orderItem.getProductName(), orderItem.getQuantity(), orderItem.getProductId(), orderItem.getPricePerUnit())).toList()
        );

        try {
            return restClient.post()
                    .uri("/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new RuntimeException(
                                "Payment failed: HTTP " + res.getStatusCode()
                        );
                    })
                    .body(PaymentResponse.class);

        } catch (RestClientException ex) {
            log.error("[CRITICAL] Payment-Service ist DOWN für Order={}: {}", order.getId(), ex.getMessage());
            throw ex;
        }
    }
}