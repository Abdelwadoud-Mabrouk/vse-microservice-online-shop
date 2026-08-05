package com.vseonlineshop.shopservice.service.client;

import com.vseonlineshop.shopservice.dto.payment.PaymentRequest;
import com.vseonlineshop.shopservice.dto.payment.PaymentResponse;
import com.vseonlineshop.shopservice.dto.payment.RefundRequestDto;
import com.vseonlineshop.shopservice.dto.payment.RefundResponseDto;
import com.vseonlineshop.shopservice.exception.PaymentServiceException;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.vseonlineshop.shopservice.config.LogUtils.safeContext;
import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
@Slf4j
public class BillingGateway {

    private final RestClient restClient;

    public BillingGateway(@Qualifier("paymentRestClient") RestClient restClient) {
        this.restClient = restClient;
    }


    @Retry(name = "payment")
    public PaymentResponse pay(UUID correlationId, PaymentRequest request, UUID orderId) {

        log.info(
                "Billing Payment Request gestartet",
                kv("correlationId", correlationId),
                kv("service", "shop-service"),
                kv("context", safeContext(
                        "orderId", orderId,
                        "operation", "POST /pay"
                ))
        );

        try {
            PaymentResponse response = restClient.post()
                    .uri("/pay")
                    .header("X-Correlation-Id", Objects.toString(correlationId, ""))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new HttpServerErrorException(res.getStatusCode(), "Billing 5xx Server Error");
                    })
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new PaymentServiceException("Billing 4xx Client Error");
                    })
                    .body(PaymentResponse.class);

            assert response != null;
            log.info(
                    "Billing Payment Response erhalten",
                    kv("correlationId", correlationId),
                    kv("service", "shop-service"),
                    kv("context", safeContext(
                            "orderId", orderId,
                            "paymentStatus", response.status()
                    ))
            );

            return response;

        } catch (ResourceAccessException | HttpServerErrorException ex) {
            log.error(
                    "Billing Service technisch nicht erreichbar",
                    kv("correlationId", correlationId),
                    kv("service", "shop-service"),
                    kv("context", safeContext(
                            "orderId", orderId,
                            "exception", ex.getClass().getSimpleName(),
                            "message", ex.getMessage()
                    ))
            );
            throw ex;

        } catch (PaymentServiceException ex) {
            log.warn(
                    "Billing Payment fachlich abgelehnt",
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

    @Retry(name = "refund")
    public RefundResponseDto refund(UUID correlationId, RefundRequestDto request, UUID orderId) {

        log.info(
                "Billing Refund Request gestartet",
                kv("correlationId", correlationId),
                kv("service", "shop-service"),
                kv("context", safeContext(
                        "orderId", orderId,
                        "operation", "POST /refund"
                ))
        );

        try {
            RefundResponseDto response = restClient.post()
                    .uri("/refund")
                    .header("X-Correlation-Id", Objects.toString(correlationId, ""))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new HttpServerErrorException(res.getStatusCode(), "Billing 5xx Server Error");
                    })
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new PaymentServiceException("Refund abgelehnt HTTP " + res.getStatusCode());
                    })
                    .body(RefundResponseDto.class);

            assert response != null;
            log.info(
                    "Billing Refund erfolgreich",
                    kv("correlationId", correlationId),
                    kv("service", "shop-service"),
                    kv("context", safeContext(
                            "orderId", orderId,
                            "refundStatus", response.status()
                    ))
            );

            return response;

        } catch (ResourceAccessException | HttpServerErrorException ex) {
            log.error(
                    "Billing Refund technisch fehlgeschlagen",
                    kv("correlationId", correlationId),
                    kv("service", "shop-service"),
                    kv("context", safeContext(
                            "orderId", orderId,
                            "exception", ex.getClass().getSimpleName(),
                            "message", ex.getMessage()
                    ))
            );
            throw ex;

        } catch (PaymentServiceException ex) {
            log.warn(
                    "Billing Refund fachlich abgelehnt",
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

    @Retry(name = "paypalCapture")
    public Map<String, String> capturePaypal(String token) {

        try {
            log.info(
                    "PayPal Capture gestartet",
                    kv("service", "shop-service"),
                    kv("context", safeContext(
                            "operation", "POST /paypal/capture",
                            "token", token
                    ))
            );

            Map<String, String> response = restClient.post()
                    .uri("/paypal/capture/{token}", Objects.toString(token, ""))
                    .contentType(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new HttpServerErrorException(res.getStatusCode());
                    })
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new PaymentServiceException("PayPal Capture abgelehnt HTTP " + res.getStatusCode());
                    })
                    .body(new ParameterizedTypeReference<>() {});

            String status = (response != null) ? response.get("message") : "UNKNOWN";

            log.info(
                    "PayPal Capture erfolgreich",
                    kv("service", "shop-service"),
                    kv("context", safeContext(
                            "status", status
                    ))
            );

            return response;

        } catch (ResourceAccessException | HttpServerErrorException ex) {
            log.error(
                    "PayPal Capture technisch fehlgeschlagen",
                    kv("service", "shop-service"),
                    kv("context", safeContext(
                            "exception", ex.getClass().getSimpleName(),
                            "message", ex.getMessage()
                    ))
            );
            throw ex;

        } catch (PaymentServiceException ex) {
            log.warn(
                    "PayPal Capture fachlich abgelehnt",
                    kv("service", "shop-service"),
                    kv("context", safeContext(
                            "reason", ex.getMessage()
                    ))
            );
            throw ex;
        }
    }
}