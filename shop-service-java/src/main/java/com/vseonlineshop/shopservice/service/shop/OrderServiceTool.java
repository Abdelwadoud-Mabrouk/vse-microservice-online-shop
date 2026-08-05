package com.vseonlineshop.shopservice.service.shop;

import com.vseonlineshop.shopservice.domain.*;
import com.vseonlineshop.shopservice.dto.order.response.*;
import com.vseonlineshop.shopservice.dto.warehouse.ReservationResponse;
import com.vseonlineshop.shopservice.event.dtos.system.SystemAlertPayload;
import com.vseonlineshop.shopservice.event.producer.KafkaEventPublisher;
import com.vseonlineshop.shopservice.repository.OrderRepository;
import com.vseonlineshop.shopservice.service.client.WarehouseClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.vseonlineshop.shopservice.config.LogUtils.safeContext;
import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceTool {

    private final OrderRepository orderRepository;
    private final WarehouseClient warehouseClient;
    private final KafkaEventPublisher kafkaEventPublisher;

    public void cancelOrder(Order order, UUID reservationOrderId, OrderStatus targetStatus, String reason, Throwable exception) {

        if (order != null) {
            try {
                order.setStatus(targetStatus);
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);
                log.info(
                        "Order in DB erfolgreich aktualisiert",
                        kv("correlationId", order.getCorrelationId()),
                        kv("service", "shop-service"),
                        kv("context", safeContext(
                                "orderId", order.getId(),
                                "targetStatus", targetStatus
                        ))
                );
            } catch (Exception dbEx) {
                log.error(
                        "Order Status konnte nicht gespeichert werden",
                        kv("correlationId", order.getCorrelationId()),
                        kv("service", "shop-service"),
                        kv("context", safeContext(
                                "orderId", order.getId(),
                                "targetStatus", targetStatus,
                                "errorMessage", dbEx.getMessage()
                        )),
                        dbEx
                );
            }

            // Event über Kafka publizieren
            safePublish(() -> kafkaEventPublisher.publishOrderCancelled(order, reason));

            // Lager-Reservierung stornieren
            if (reservationOrderId != null) {
                log.info(
                        "Warehouse Reservation Rollback gestartet",
                        kv("correlationId", order.getCorrelationId()),
                        kv("service", "shop-service"),
                        kv("context", safeContext(
                                "orderId", order.getId(),
                                "reservationId", reservationOrderId,
                                "operation", "RESERVATION_CANCEL"
                        ))
                );
                safePublish(() -> kafkaEventPublisher.publishReservationRollbackRequested(order));

                try {
                    ReservationResponse response = warehouseClient.cancelReservation(order);

                    Map<String, Object> context = new HashMap<>();

                    context.put("reservationId", response!=null?response.reservationId():null);
                    context.put("orderId", response!=null?response.orderId():null);
                    context.put("status",  response!=null?response.status(): null);


                    log.info(
                            "Warehouse Reservierung erfolgreich freigegeben",
                            kv("correlationId", order.getCorrelationId()),
                            kv("service", "shop-service"),
                            kv("context", context)
                    );
                } catch (Exception ex) {
                    Map<String, Object> context = new HashMap<>();

                    context.put("reservationId", reservationOrderId);
                    context.put("orderId", order.getId());
                    context.put("errorMessage", ex.getMessage());


                    log.error(
                            "Warehouse Rollback fehlgeschlagen",
                            kv("correlationId", order.getCorrelationId()),
                            kv("service", "shop-service"),
                            kv("context", context),
                            ex
                    );
                }
            }
        }

        if (targetStatus != OrderStatus.OUT_OF_STOCK) {
            Map<String, String> metadata = Map.of(
                    "correlationId", order != null ? order.getCorrelationId().toString() : "Unknown",
                    "exceptionClass", exception != null ? exception.getClass().getSimpleName() : "Unknown"
            );

            SystemAlertPayload alertPayload = new SystemAlertPayload(
                    UUID.randomUUID().toString(),
                    "shop-service",
                    order != null ? order.getId().toString() : "N/A",
                    "Kritischer Ausfall! Grund: " + reason + " | Details: " + (exception != null ? exception.getMessage() : "Keine"),
                    Instant.now(),
                    metadata
            );

            safePublish(() -> kafkaEventPublisher.publishSystemAlert(order, alertPayload));
        } else {
            log.error(
                    "Kein SystemAlert gesendet (fachlicher Fehler)",
                    kv("correlationId", order != null ? order.getCorrelationId().toString() : "Unknown"),
                    kv("service", "shop-service"),
                    kv("context", safeContext(
                            "orderId", order != null ? order.getId().toString() : "Unknown",
                            "reason", "OUT_OF_STOCK"
                    ))
            );
        }
    }


    public OrderDetailsResponse toResponse(Order order) {

        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getProductId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getPricePerUnit(),
                        item.getSubTotal(),
                        item.getTaxRate(),
                        item.getTaxAmount(),
                        item.getTotalPrice()
                ))
                .toList();


        CustomerInformation customer = order.getCustomer();

        CustomerInformationResponse customerResponse =
                new CustomerInformationResponse(
                        customer.getFirstName(),
                        customer.getLastName(),
                        customer.getEmail(),
                        customer.getPhone()
                );


        ShippingAddress shippingAddress = order.getShippingAddress();

        ShippingAddressResponse shippingAddressResponse =
                new ShippingAddressResponse(
                        shippingAddress.getStreet(),
                        shippingAddress.getHouseNumber(),
                        shippingAddress.getPostalCode(),
                        shippingAddress.getCity(),
                        shippingAddress.getCountry()
                );


        return new OrderDetailsResponse(
                order.getId(),
                customerResponse,
                shippingAddressResponse,
                itemResponses,
                order.getTotalAmount(),
                order.getPaymentMethod(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }

    public void safePublish(Runnable publishAction) {
        try {
            publishAction.run();
        } catch (Exception e) {
            log.error("Kafka Event konnte nicht gesendet werden",
                    kv("context", Map.of(
                            "errorMessage", e.getMessage()
                    )),
                    e
            );
        }
    }
}
