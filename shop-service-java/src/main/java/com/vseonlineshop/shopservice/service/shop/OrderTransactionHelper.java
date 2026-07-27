package com.vseonlineshop.shopservice.service.shop;

import com.vseonlineshop.shopservice.domain.*;
import com.vseonlineshop.shopservice.dto.order.request.CustomerInformationRequest;
import com.vseonlineshop.shopservice.dto.order.request.ItemRequest;
import com.vseonlineshop.shopservice.dto.order.request.OrderRequest;
import com.vseonlineshop.shopservice.dto.order.request.ShippingAddressRequest;
import com.vseonlineshop.shopservice.dto.order.response.CheckoutResponse;
import com.vseonlineshop.shopservice.dto.payment.PaymentResponse;
import com.vseonlineshop.shopservice.product.domain.Product;
import com.vseonlineshop.shopservice.product.repository.ProductRepository;
import com.vseonlineshop.shopservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderTransactionHelper {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Order createInitialOrder(OrderRequest request, UUID correlationId) {
        Order order = buildInitialOrder(request, correlationId);
        return orderRepository.save(order);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Order updateToProcessing(Order order, UUID reservationId) {
        Order orderDb = orderRepository.findById(order.getId()).orElseThrow();
        BigDecimal total = calculateTotal(orderDb.getItems());
        orderDb.setTotalAmount(total);
        orderDb.setStatus(OrderStatus.PROCESSING);
        orderDb.setReservationId(reservationId);
        orderDb.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(orderDb);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CheckoutResponse updateToAwaitingPayment(UUID orderId, PaymentResponse payment) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.AWAITING_PAYMENT);
        order.setPaymentId(payment.paymentId().toString());
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        return toResponse(order, payment.approvalUrl());
    }

    public BigDecimal calculateTotal(List<OrderItem> items) {
        return items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private CustomerInformation toCustomer(CustomerInformationRequest request) {

        CustomerInformation customer = new CustomerInformation();

        customer.setFirstName(request.firstName());
        customer.setLastName(request.lastName());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());

        return customer;
    }

    private ShippingAddress toShippingAddress(ShippingAddressRequest request) {

        ShippingAddress address = new ShippingAddress();

        address.setStreet(request.street());
        address.setHouseNumber(request.houseNumber());
        address.setPostalCode(request.postalCode());
        address.setCity(request.city());
        address.setCountry(request.country());

        return address;
    }

    private Order buildInitialOrder(OrderRequest request, UUID correlationId) {

        Order order = new Order();
        order.setCorrelationId(correlationId);
        order.setCurrency(request.currency());
        order.setPaymentMethod(request.paymentMethod());
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(BigDecimal.ZERO);

        order.setCustomer(toCustomer(request.customer()));
        order.setShippingAddress(toShippingAddress(request.shippingAddress()));

        BigDecimal orderTotal = BigDecimal.ZERO;

        for (ItemRequest whItem : request.items()) {

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProductId(whItem.productId());
            item.setQuantity(whItem.quantity());
            item.setSku(whItem.sku());

            Product product = productRepository.findById(whItem.productId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found"));

            BigDecimal price = product.getPrice();
            item.setPricePerUnit(price);
            item.setProductName(product.getName());

            BigDecimal subTotal = price.multiply(BigDecimal.valueOf(whItem.quantity()));

            item.setTaxRate(PricingPolicy.taxRate());
            BigDecimal taxAmount = subTotal.multiply(item.getTaxRate());
            BigDecimal totalPrice = subTotal.add(taxAmount);

            item.setSubTotal(subTotal);
            item.setTaxAmount(taxAmount);
            item.setTotalPrice(totalPrice);

            orderTotal = orderTotal.add(totalPrice);

            order.addItem(item);
        }

        order.setTotalAmount(orderTotal);

        return order;
    }


    private CheckoutResponse toResponse(Order order, String approvalUrl) {
        return new CheckoutResponse(
                order.getId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCurrency(),
                approvalUrl
        );
    }
}