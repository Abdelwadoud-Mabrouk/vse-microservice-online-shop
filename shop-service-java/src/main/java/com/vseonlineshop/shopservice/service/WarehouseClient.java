package com.vseonlineshop.shopservice.service;


import com.vseonlineshop.shopservice.domain.Order;
import com.vseonlineshop.shopservice.dto.warehouse.ReservationRequest;
import com.vseonlineshop.shopservice.dto.warehouse.ReservationRequestItem;
import com.vseonlineshop.shopservice.dto.warehouse.ReservationResponse;
import com.vseonlineshop.shopservice.dto.warehouse.ReservationResponseItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Mock-Implementation des Warehouse-Clients.
 * 
 * Echte Implementierung: RestTemplate / WebClient auf ${warehouse.base-url}/reservations
 * Mock gibt die Beispiel-Response zurück und leitet productName + unitPrice
 * aus einer statischen Produktdatenbank ab.
 *
 * Ersetze die mock*-Methoden gegen einen echten HTTP-Call, sobald
 * das Warehouse-Service deployed ist.
 */
@Service
@Slf4j
public class WarehouseClient {


    /**
     * Sendet POST /reservations an Warehouse und gibt die Antwort zurück.
     *
     *   - Unbekannte productIds bekommen Name "Unknown-{id}" und Preis 0.00
     *   - Gibt nie einen Fehler zurück (Warehouse ist immer verfügbar im Mock)
     */
    public ReservationResponse reserve(Order order) {
        log.info("[Warehouse-Mock] Reservierung für Order {} (correlationId={})",
            order.getId(), order.getCorrelationId());

        ReservationRequest request = buildRequest(order);
        log.debug("[Warehouse] Request: {}", request);

        log.info("REST Sync Call to Warehouse-Service for Order: {}", request.orderId());


        List<ReservationResponseItem> items = request.items().stream().map(i -> {

            String name = i.productId().toString().startsWith("a") ? "Nike Air Max" : "Adidas Hose";


            return new ReservationResponseItem(i.productId(), name, i.quantity());

        }).toList();


        ReservationResponse response=  new ReservationResponse(UUID.randomUUID(), request.orderId(), items);



        log.info("[Warehouse-Mock] Reservierung erfolgreich, {} Items", items.size());
        return response;
    }

    private ReservationRequest buildRequest(Order order) {
        List<ReservationRequestItem> items = order.getItems().stream()
            .map(i -> new ReservationRequestItem(i.getProductId(), i.getQuantity()))
            .toList();
        return new ReservationRequest(order.getCorrelationId(), order.getId(), items);
    }
}