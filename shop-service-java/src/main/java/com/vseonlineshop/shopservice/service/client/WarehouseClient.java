package com.vseonlineshop.shopservice.service.client;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.vseonlineshop.shopservice.domain.Order;
import com.vseonlineshop.shopservice.dto.warehouse.ReservationRequest;
import com.vseonlineshop.shopservice.dto.warehouse.ReservationRequestItem;
import com.vseonlineshop.shopservice.dto.warehouse.ReservationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;



@Service
@Slf4j
public class WarehouseClient {

    private final RestClient restClient;

    // Die URL wird analog zum PaymentClient aus den application.properties geladen
    public WarehouseClient(@Value("${services.warehouse.url}") String warehouseServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(warehouseServiceUrl)
                .build();
    }

    /**
     * Sendet POST /reservations an den echten Warehouse-Service via RestClient.
     */
    public ReservationResponse reserve(Order order) {
        log.info("[WarehouseClient] Synchroner Call für Order={}", order.getId());

        // Request-Payload aufbauen
        ReservationRequest request = buildRequest(order);
        log.debug("[WarehouseClient] Request Payload: {}", request);

        try{
            ObjectMapper mapper = new ObjectMapper();

            String json = mapper.writeValueAsString(request);

            System.out.println(json);
        }catch (Exception ex){
            System.out.println("[WarehouseClient] Unerwarteter Fehler");
        }

        try {
            return restClient.post()
                    .uri("/reservations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Correlation-Id", order.getCorrelationId().toString())
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new RuntimeException(
                                "Warehouse reservation failed: HTTP " + res.getStatusCode() +"_"+res.getBody()
                        );
                    })
                    .body(ReservationResponse.class);

        } catch (RestClientException ex) {
            log.error("[CRITICAL] Warehouse-Service ist DOWN für Order={}: {}", order.getId(), ex.getMessage());
            throw ex;
        }
    }

    private ReservationRequest buildRequest(Order order) {
        List<ReservationRequestItem> items = order.getItems().stream()
                .map(i -> new ReservationRequestItem(i.getProductId(), i.getQuantity(),i.getSku()))
                .toList();
        return new ReservationRequest( order.getId(), items);
    }
}