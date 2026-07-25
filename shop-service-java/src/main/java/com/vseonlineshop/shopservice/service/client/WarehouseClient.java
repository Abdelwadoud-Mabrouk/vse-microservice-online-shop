package com.vseonlineshop.shopservice.service.client;

import com.vseonlineshop.shopservice.domain.Order;
import com.vseonlineshop.shopservice.dto.warehouse.*;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Service
@Slf4j
public class WarehouseClient {

    private final RestClient restClient;

    public WarehouseClient(@Value("${services.warehouse.url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Retry(name = "warehouse")
    public ReservationResponse reserve(Order order) {
        return post("/reservations",
                order,
                buildRequest(order),
                ReservationResponse.class);
    }

    @Retry(name = "warehouse")
    public ReservationCommitedResponse reservationCommited(Order order) {
        return post("/reservations/{id}/commit",
                order,
                buildRequest(order),
                ReservationCommitedResponse.class,
                order.getReservationId());
    }

    @Retry(name = "warehouse")
    public ReservationResponse cancelReservation(Order order) {
        return delete(
                order,
                order.getReservationId()
        );
    }

    private <T> T delete(Order order, Object... uriVars) {
        log.info("[WarehouseClient] DELETE /reservations/{} for order={}", uriVars, order.getId());

        try {
            return restClient.delete()
                    .uri("/reservations/{id}", uriVars)
                    .header("X-Correlation-Id", order.getCorrelationId().toString())
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new HttpServerErrorException(res.getStatusCode(), "Warehouse 5xx Server Error");
                    })
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new HttpClientErrorException(res.getStatusCode(), "Warehouse 4xx Client Error");
                    })
                    .body((Class<T>) ReservationResponse.class);

        } catch (ResourceAccessException | HttpServerErrorException ex) {
            log.error("[WarehouseClient] Temporärer Warehouse Fehler (wird per Retry wiederholt) für order={}: {}",
                    order.getId(), ex.getMessage());
            throw ex;
        } catch (RestClientException ex) {
            log.error("[WarehouseClient] Warehouse Business/Client Fehler (kein Retry) für order={}: {}",
                    order.getId(), ex.getMessage());
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
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new HttpServerErrorException(res.getStatusCode(), "Warehouse 5xx Server Error");
                    })
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new HttpClientErrorException(res.getStatusCode(), "Warehouse 4xx Client Error");
                    })
                    .body(responseType);

        } catch (ResourceAccessException | HttpServerErrorException ex) {
            log.error("[WarehouseClient] Temporärer Warehouse Fehler (wird per Retry wiederholt) für order={}: {}",
                    order.getId(), ex.getMessage());
            throw ex;
        } catch (RestClientException ex) {
            log.error("[WarehouseClient] Warehouse Business/Client Fehler (kein Retry) für order={}: {}",
                    order.getId(), ex.getMessage());
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