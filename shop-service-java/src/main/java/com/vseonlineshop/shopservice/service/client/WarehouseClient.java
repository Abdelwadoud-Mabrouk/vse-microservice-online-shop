package com.vseonlineshop.shopservice.service.client;

import com.vseonlineshop.shopservice.domain.Order;
import com.vseonlineshop.shopservice.dto.warehouse.ReservationCommitedResponse;
import com.vseonlineshop.shopservice.dto.warehouse.ReservationRequest;
import com.vseonlineshop.shopservice.dto.warehouse.ReservationRequestItem;
import com.vseonlineshop.shopservice.dto.warehouse.ReservationResponse;
import com.vseonlineshop.shopservice.exception.WarehouseServiceException;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.vseonlineshop.shopservice.config.LogUtils.safeContext;
import static net.logstash.logback.argument.StructuredArguments.kv; // Oder deine safeContext/kv Importe

@Service
@Slf4j
public class WarehouseClient {

    private final RestClient restClient;

    public WarehouseClient(@Qualifier("warehouseRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Retry(name = "warehouseReserve")
    public ReservationResponse reserve(Order order) {
        UUID correlationId = order.getCorrelationId();
        UUID orderId = order.getId();

        log.info(
                "Warehouse Reserve Request gestartet",
                kv("correlationId", correlationId),
                kv("service", "shop-service"),
                kv("context", safeContext(
                        "orderId", orderId,
                        "operation", "POST /reservations"
                ))
        );

        try {
            ReservationResponse response = restClient.post()
                    .uri("/reservations")
                    .header("X-Correlation-Id", Objects.toString(correlationId, ""))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildRequest(order))
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new HttpServerErrorException(res.getStatusCode(), "Warehouse 5xx Server Error");
                    })
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new WarehouseServiceException("Warehouse Reserve abgelehnt HTTP " + res.getStatusCode());
                    })
                    .body(ReservationResponse.class);

            log.info(
                    "Warehouse Reserve erfolgreich",
                    kv("correlationId", correlationId),
                    kv("service", "shop-service"),
                    kv("context", safeContext(
                            "orderId", orderId,
                            "reservationId", response!=null?response.reservationId():null
                    ))
            );

            return response;

        } catch (ResourceAccessException | HttpServerErrorException ex) {
            log.error(
                    "Warehouse Service technisch nicht erreichbar",
                    kv("correlationId", correlationId),
                    kv("service", "shop-service"),
                    kv("context", safeContext(
                            "orderId", orderId,
                            "exception", ex.getClass().getSimpleName(),
                            "message", ex.getMessage()
                    ))
            );
            throw ex;

        } catch (WarehouseServiceException ex) {
            log.warn(
                    "Warehouse Reserve fachlich abgelehnt",
                    kv("correlationId", correlationId),
                    kv("service", "shop-service"),
                    kv("context", safeContext(
                            "orderId", orderId,
                            "reason", ex.getMessage()
                    ))
            );
            throw ex;
        }
    }

    @Retry(name = "warehouseCommit")
    public ReservationCommitedResponse reservationCommited(Order order) {
        UUID correlationId = order.getCorrelationId();
        UUID orderId = order.getId();

        log.info(
                "Warehouse Commit Request gestartet",
                kv("correlationId", correlationId),
                kv("service", "shop-service"),
                kv("context", safeContext(
                        "orderId", orderId,
                        "operation", "POST /reservations/{id}/commit"
                ))
        );

        try {
            ReservationCommitedResponse response = restClient.post()
                    .uri("/reservations/{id}/commit", order.getReservationId())
                    .header("X-Correlation-Id", Objects.toString(correlationId, ""))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildRequest(order))
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new HttpServerErrorException(res.getStatusCode(), "Warehouse 5xx Server Error");
                    })
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new WarehouseServiceException("Warehouse Commit abgelehnt HTTP " + res.getStatusCode());
                    })
                    .body(ReservationCommitedResponse.class);

            log.info(
                    "Warehouse Commit erfolgreich",
                    kv("correlationId", correlationId),
                    kv("service", "shop-service"),
                    kv("context", safeContext(
                            "orderId", orderId,
                            "status", response!=null?response.status():null
                    ))
            );

            return response;

        } catch (ResourceAccessException | HttpServerErrorException ex) {
            log.error(
                    "Warehouse Commit technisch fehlgeschlagen",
                    kv("correlationId", correlationId),
                    kv("service", "shop-service"),
                    kv("context", safeContext(
                            "orderId", orderId,
                            "exception", ex.getClass().getSimpleName(),
                            "message", ex.getMessage()
                    ))
            );
            throw ex;

        } catch (WarehouseServiceException ex) {
            log.warn(
                    "Warehouse Commit fachlich abgelehnt",
                    kv("correlationId", correlationId),
                    kv("service", "shop-service"),
                    kv("context", safeContext(
                            "orderId", orderId,
                            "reason", ex.getMessage()
                    ))
            );
            throw ex;
        }
    }

    @Retry(name = "warehouseCancel")
    public ReservationResponse cancelReservation(Order order) {
        UUID correlationId = order.getCorrelationId();
        UUID orderId = order.getId();

        log.info(
                "Warehouse Delete Request gestartet",
                kv("correlationId", correlationId),
                kv("service", "shop-service"),
                kv("context", safeContext(
                        "orderId", orderId,
                        "operation", "DELETE /reservations/{id}"
                ))
        );

        try {
            ReservationResponse response = restClient.delete()
                    .uri("/reservations/{id}", order.getReservationId())
                    .header("X-Correlation-Id", Objects.toString(correlationId, ""))
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new HttpServerErrorException(res.getStatusCode(), "Warehouse 5xx Server Error");
                    })
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new WarehouseServiceException("Warehouse Cancel abgelehnt HTTP " + res.getStatusCode());
                    })
                    .body(ReservationResponse.class);
            log.info(
                    "Warehouse Stornierung erfolgreich",
                    kv("correlationId", correlationId),
                    kv("service", "shop-service"),
                    kv("context", safeContext(
                            "orderId", orderId
                    ))
            );

            return response;
        } catch (ResourceAccessException | HttpServerErrorException ex) {
            log.error(
                    "Warehouse Stornierung technisch fehlgeschlagen",
                    kv("correlationId", correlationId),
                    kv("service", "shop-service"),
                    kv("context", safeContext(
                            "orderId", orderId,
                            "exception", ex.getClass().getSimpleName(),
                            "message", ex.getMessage()
                    ))
            );
            throw ex;

        } catch (WarehouseServiceException ex) {
            log.warn(
                    "Warehouse Stornierung fachlich abgelehnt",
                    kv("correlationId", correlationId),
                    kv("service", "shop-service"),
                    kv("context", safeContext(
                            "orderId", orderId,
                            "reason", ex.getMessage()
                    ))
            );
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