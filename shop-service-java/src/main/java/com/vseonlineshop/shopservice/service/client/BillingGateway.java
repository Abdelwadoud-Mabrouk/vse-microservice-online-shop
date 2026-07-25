package com.vseonlineshop.shopservice.service.client;

import com.vseonlineshop.shopservice.dto.payment.PaymentRequest;
import com.vseonlineshop.shopservice.dto.payment.PaymentResponse;
import com.vseonlineshop.shopservice.dto.payment.RefundRequestDto;
import com.vseonlineshop.shopservice.dto.payment.RefundResponseDto;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class BillingGateway {


    private final RestClient restClient;


    @Retry(name = "payment")
    public PaymentResponse pay(PaymentRequest request, UUID orderId) {

        try {

            log.info(
                    "[PaymentGateway] Payment Request senden für Order={}",
                    orderId
            );


            return restClient.post()
                    .uri("/pay")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()

                    .onStatus(
                            HttpStatusCode::is5xxServerError,
                            (req, res) -> {
                                throw new HttpServerErrorException(
                                        res.getStatusCode()
                                );
                            }
                    )

                    .onStatus(
                            HttpStatusCode::is4xxClientError,
                            (req, res) -> {
                                throw new RestClientException(
                                        "Client error HTTP " + res.getStatusCode()
                                );
                            }
                    )
                    .body(PaymentResponse.class);


        } catch (ResourceAccessException | HttpServerErrorException ex) {

            log.error(
                    "[PaymentGateway] Temporärer Billing Fehler Order={}: {}",
                    orderId,
                    ex.getMessage()
            );

            throw ex;
        } catch (RestClientException ex) {

            log.error(
                    "[PaymentGateway] Billing Business Fehler Order={}: {}",
                    orderId,
                    ex.getMessage()
            );

            throw ex;
        }
    }


    @Retry(name = "paypalCapture")
    public Map<String, String> capturePaypal(String token) {


        try {

            return restClient.post()
                    .uri("/paypal/capture/{token}", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .retrieve()

                    .onStatus(
                            HttpStatusCode::isError,
                            (req, res) -> {

                                throw new RestClientException(
                                        "PayPal Capture failed HTTP "
                                                + res.getStatusCode()
                                );
                            })

                    .body(new ParameterizedTypeReference<>() {
                    });


        } catch (RestClientException ex) {

            log.error(
                    "[PaymentGateway] PayPal Capture Fehler Token={}: {}",
                    token,
                    ex.getMessage()
            );

            throw ex;
        }
    }

    @Retry(name = "payment")
    public RefundResponseDto refund(RefundRequestDto request, UUID orderId) {
        try {
            log.info(
                    "[BillingGateway] Refund Request senden für Order={}, CorrelationId={}",
                    orderId, request.correlationId()
            );

            return restClient.post()
                    .uri("/refund")
                    .body(request)
                    .retrieve()
                    .body(RefundResponseDto.class);

        } catch (RestClientException ex) {
            log.error("[BillingGateway] Fehler beim Refund-Aufruf für Order={}. Grund: {}", orderId, ex.getMessage());
            throw ex;
        }
    }
}
