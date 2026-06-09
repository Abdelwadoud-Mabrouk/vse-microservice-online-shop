package com.vseonlineshop.shopservice.service.client;


import com.vseonlineshop.shopservice.domain.Order;
import com.vseonlineshop.shopservice.dto.warehouse.ReservationCommitedResponse;
import com.vseonlineshop.shopservice.dto.warehouse.ReservationRequest;
import com.vseonlineshop.shopservice.dto.warehouse.ReservationRequestItem;
import com.vseonlineshop.shopservice.dto.warehouse.ReservationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;


@Service
@Slf4j
public class WarehouseClient {


    private final RestClient restClient;

    public WarehouseClient(@Value("${services.warehouse.url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public ReservationResponse reserve(Order order) {
        return post("/reservations",
                order,
                buildRequest(order),
                ReservationResponse.class);
    }

    public ReservationCommitedResponse reservationCommited(Order order) {
        return post("/reservations/{id}/commit",
                order,
                buildRequest(order),
                ReservationCommitedResponse.class,
                order.getReservationId());
    }

    public ReservationResponse cancelReservation(Order order) {
        return delete(
                order,
                order.getReservationId()
        );
    }

    private <T> T delete(Order order,
                         Object... uriVars) {

        log.info("[WarehouseClient] DELETE {} for order={}", "/reservations/{id}", order.getId());

        try {
            return restClient.delete()
                    .uri("/reservations/{id}", uriVars)
                    .header("X-Correlation-Id", order.getCorrelationId().toString())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new RuntimeException(
                                "Warehouse call failed: HTTP " + res.getStatusCode()
                        );
                    })
                    .body((Class<T>) ReservationResponse.class);

        } catch (RestClientException ex) {
            log.error(
                    "[CRITICAL] Warehouse-Service DOWN while cancelling reservation for order={}",
                    order.getId(),
                    ex
            );
            throw ex;
        }
    }


    private <T> T post(String uri,
                       Order order,
                       Object body,
                       Class<T> responseType,
                       Object... uriVars) {

        log.info("[WarehouseClient] POST {} for order={}", uri, order.getId());
        log.debug("[WarehouseClient] Payload: {}", body);

        try {
            return restClient.post()
                    .uri(uri, uriVars)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Correlation-Id", order.getCorrelationId().toString())
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new RuntimeException(
                                "Warehouse call failed: HTTP " + res.getStatusCode()
                        );
                    })
                    .body(responseType);

        } catch (RestClientException ex) {
            log.error("[CRITICAL] Warehouse-Service DOWN for order={}", order.getId(), ex);
            throw ex;
        }
    }

    private ReservationRequest buildRequest(Order order) {
        List<ReservationRequestItem> items = order.getItems().stream()
                .map(i -> new ReservationRequestItem(i.getProductId(), i.getQuantity(), i.getSku()))
                .toList();
        return new ReservationRequest(order.getId(), items);
    }
}