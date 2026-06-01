package com.vseonlineshop.shopservice.service.shop;

import com.vseonlineshop.shopservice.domain.Order;
import com.vseonlineshop.shopservice.domain.OrderItem;
import com.vseonlineshop.shopservice.domain.OrderStatus;
import com.vseonlineshop.shopservice.domain.Product;
import com.vseonlineshop.shopservice.dto.payment.PaymentResponse;
import com.vseonlineshop.shopservice.dto.shop.ItemRequest;
import com.vseonlineshop.shopservice.dto.shop.OrderItemResponse;
import com.vseonlineshop.shopservice.dto.shop.OrderRequest;
import com.vseonlineshop.shopservice.dto.shop.OrderResponse;
import com.vseonlineshop.shopservice.repository.OrderRepository;
import com.vseonlineshop.shopservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderTransactionHelper {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Order createInitialOrder(OrderRequest request) {
        UUID correlationId = UUID.randomUUID();
        Order order = buildInitialOrder(request, correlationId);
        return orderRepository.save(order);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Order updateToProcessing(UUID orderId, BigDecimal total) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setTotalAmount(total);
        order.setStatus(OrderStatus.PROCESSING);
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OrderResponse updateToAwaitingPayment(UUID orderId, PaymentResponse payment) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.AWAITING_PAYMENT);
        order.setPaymentId(payment.paymentId().toString());
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        return toResponse(order, payment.approvalUrl());
    }

    private Order buildInitialOrder(OrderRequest request, UUID correlationId) {
        Order order = new Order();
        order.setCorrelationId(correlationId);
        order.setUserId(request.userId());
        order.setCurrency(request.currency());
        order.setPaymentMethod(request.paymentMethod());
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(BigDecimal.ZERO);

        for (ItemRequest whItem : request.items()) {
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProductId(whItem.productId());
            item.setQuantity(whItem.quantity());
            item.setSku(whItem.sku());
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



    private OrderResponse toResponse(Order order, String url) {
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
                order.getPaymentMethod(),
                itemResponses,
                order.getCreatedAt(),
                order.getUpdatedAt(),
                url
        );
    }
}