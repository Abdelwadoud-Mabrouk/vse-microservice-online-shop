package com.vseonlineshop.shopservice.service;

import com.vseonlineshop.shopservice.domain.Order;
import com.vseonlineshop.shopservice.domain.OrderItem;
import com.vseonlineshop.shopservice.domain.OrderStatus;
import com.vseonlineshop.shopservice.domain.Product;
import com.vseonlineshop.shopservice.dto.payment.PaymentResponse;
import com.vseonlineshop.shopservice.dto.shop.ItemRequest;
import com.vseonlineshop.shopservice.dto.shop.OrderItemResponse;
import com.vseonlineshop.shopservice.dto.shop.OrderRequest;
import com.vseonlineshop.shopservice.dto.shop.OrderResponse;
import com.vseonlineshop.shopservice.dto.warehouse.*;
import com.vseonlineshop.shopservice.exception.BusinessException;
import com.vseonlineshop.shopservice.exception.OrderNotFoundException;
import com.vseonlineshop.shopservice.exception.PaymentServiceException;
import com.vseonlineshop.shopservice.exception.ServiceUnavailableException;
import com.vseonlineshop.shopservice.repository.OrderRepository;
import com.vseonlineshop.shopservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderApplicationService {

    private static final Logger log = LoggerFactory.getLogger(OrderApplicationService.class);

    private final OrderRepository orderRepository;
    private final WarehouseClient warehouseClient;
    private final PaymentClient paymentClient;
    private final KafkaEventPublisher kafkaEventPublisher;
    private final ProductRepository productRepository;

    // ── Create Order ─────────────────────────────────────────────────────────

    public OrderResponse createOrder(OrderRequest request) {

        // ── 1. Order PENDING anlegen ─────────────────────────────────────────
        UUID correlationId = UUID.randomUUID();
        Order order = buildInitialOrder(request, correlationId);
        order = orderRepository.save(order);
        log.info("[OrderService] Order {} angelegt (correlationId={})", order.getId(), correlationId);

        // ── Kafka: OrderCreated (fire & forget) ──────────────────────────────
        Order finalOrder = order;
        safePublish(() -> kafkaEventPublisher.publishOrderCreated(finalOrder));


        // ── 2. Warehouse reservieren ─────────────────────────────────────────
        ReservationResponse reservation;
        try {
            reservation = warehouseClient.reserve(order);
            log.info("[OrderService] Warehouse-Reservierung erhalten, reservationId={}",
                    reservation.reservationId());
        } catch (RestClientException ex) {
            log.error("[WAREHOUSE ERROR] Warehouse unreachable for Order {}", order.getId(), ex);

            // ── Kafka: OrderCompleted (fire & forget) ─────────────────────────────
            cancelOrder(order, null, "WAREHOUSE_SERVICE_UNAVAILABLE");
            throw new ServiceUnavailableException(
                    "WAREHOUSE_SERVICE_UNAVAILABLE",
                    "Das Warenlager ist derzeit nicht erreichbar."
            );
        }

        // ── Validierung der Warehouse-Antwort ─────────────────────────────────
        if (reservation.items() == null || reservation.items().isEmpty()) {
            log.warn("[OrderService] Warehouse returned empty items for Order {}", order.getId());
            cancelOrder(order, null, "EMPTY_RESERVATION");
            throw new BusinessException("Warehouse returned an empty reservation response.");
        }

        // ── 3. Total berechnen ────────────────────────────────────────────────
        BigDecimal total = calculateTotal(order.getItems());
        order.setTotalAmount(total);
        order.setStatus(OrderStatus.PROCESSING);
        order = orderRepository.save(order);
        log.info("[OrderService] totalAmount={} {}", total, order.getCurrency());

        // ── 4. Payment ────────────────────────────────────────────────────────
        PaymentResponse payment = paymentClient.pay(order);

        if (!"CONFIRMED".equalsIgnoreCase(payment.status())) {
            // ── Kafka: OrderCompleted (fire & forget) ─────────────────────────────
            cancelOrder(order, reservation.orderId(), "PAYMENT_FAILED");
            throw new PaymentServiceException("Zahlung fehlgeschlagen – Status: " + payment.status());
        }

        // ── 5. Order COMPLETED ────────────────────────────────────────────────
        order.setStatus(OrderStatus.COMPLETED);
        order.setPaymentId(payment.paymentId());
        order = orderRepository.save(order);
        log.info("[OrderService] Order {} COMPLETED, paymentId={}", order.getId(), order.getPaymentId());

        // ── Kafka: OrderCompleted (fire & forget) ─────────────────────────────
        Order finalOrder1 = order;
        safePublish(() -> kafkaEventPublisher.publishOrderCompleted(finalOrder1));

        return toResponse(order);
    }

    // ── Get Order ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return toResponse(order);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Order buildInitialOrder(OrderRequest request, UUID correlationId) {
        Order order = new Order();
        order.setCorrelationId(correlationId);
        order.setUserId(request.userId());
        order.setCurrency(request.currency());
        order.setPaymentProvider(request.paymentProvider());
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(BigDecimal.ZERO);

        for (ItemRequest whItem : request.items()) {
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProductId(whItem.productId());
            item.setQuantity(whItem.quantity());
            Optional<Product> optionalProduct = productRepository.findById(whItem.productId());
            if (optionalProduct.isPresent()) {
                Product product = optionalProduct.get();
                BigDecimal securePriceFromShopDb = product.getPrice();
                item.setPricePerUnit(securePriceFromShopDb);
                BigDecimal subTotal = securePriceFromShopDb.multiply(BigDecimal.valueOf(whItem.quantity()));
                item.setSubTotal(subTotal);
                item.setProductName(product.getName());
            }
            order.addItem(item);
        }
        return order;
    }


    private BigDecimal calculateTotal(List<OrderItem> items) {
        return items.stream()
                .map(OrderItem::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getProductId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getPricePerUnit(),
                        item.getSubTotal()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getPaymentId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getPaymentProvider(),
                itemResponses,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private void cancelOrder(Order order, UUID reservationOrderId, String reason) {
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        safePublish(() -> kafkaEventPublisher.publishOrderCancelled(order, reason));

        if (reservationOrderId != null) {
            safePublish(() -> kafkaEventPublisher
                    .publishReservationRollbackRequested(order, reservationOrderId));
        }
    }

    private void safePublish(Runnable publishAction) {
        try {
            publishAction.run();
        } catch (Exception e) {
            // todo: Wie können wir damit umgehen, damit wir die Nachvollziehbarkeit nicht verlieren können : Transactional Outbox Pattern.
            log.error("[Kafka] Event konnte nicht gesendet werden: {}", e.getMessage(), e);
        }
    }
}