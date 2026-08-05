package com.vseonlineshop.payment.strategy.config;

import com.paypal.sdk.Environment;
import com.paypal.sdk.PaypalServerSdkClient;
import com.paypal.sdk.authentication.ClientCredentialsAuthModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.slf4j.event.Level;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaypalConfig {

    @Value("${paypal.client.id}")
    private String PAYPAL_CLIENT_ID;

    @Value("${paypal.client.secret}")
    private String PAYPAL_CLIENT_SECRET;

    @Value("${paypal.environment:sandbox}")
    private String paypalEnvironment;

    @Value("${paypal.timeout:30}")
    private long timeoutSeconds;

    Environment environment =
            "PRODUCTION".equalsIgnoreCase(paypalEnvironment)
                    ? Environment.PRODUCTION
                    : Environment.SANDBOX;


    @Bean
    public PaypalServerSdkClient paypalClient() {
        return new PaypalServerSdkClient.Builder()
                .loggingConfig(builder -> builder
                        .level(Level.DEBUG)
                        .requestConfig(logConfigBuilder -> logConfigBuilder.body(true))
                        .responseConfig(logConfigBuilder -> logConfigBuilder.headers(true)))
                .httpClientConfig(configBuilder -> configBuilder
                        .timeout(timeoutSeconds))
                .environment(environment)
                .clientCredentialsAuth(new ClientCredentialsAuthModel.Builder(
                                PAYPAL_CLIENT_ID,
                                PAYPAL_CLIENT_SECRET
                        ).build()
                )
                .build();
    }
}
