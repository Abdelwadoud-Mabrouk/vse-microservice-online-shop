package com.vseonlineshop.shopservice.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;


@Configuration
public class RestClientConfig {

    @Bean
    @Qualifier("paymentRestClient")
    public RestClient paymentRestClient(
            @Value("${services.payment.url}") String url) {

        return RestClient.builder()
                .baseUrl(url)
                .requestFactory(httpClientHttp11RequestFactory())
                .build();
    }

    @Bean
    @Qualifier("warehouseRestClient")
    public RestClient warehouseRestClient(
            @Value("${services.warehouse.url}") String url) {

        return RestClient.builder()
                .baseUrl(url)
                .requestFactory(httpClientHttp11RequestFactory())
                .build();
    }

    private JdkClientHttpRequestFactory httpClientHttp11RequestFactory() {
        HttpClient jdkHttpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        return new JdkClientHttpRequestFactory(jdkHttpClient);
    }
}