package com.vseonlineshop.payment.config;


import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${kafka.topics.payment}")
    private String paymentTopic;

    @Value("${kafka.topics.alerts}")
    private String alertTopic;

    @Bean
    public NewTopic paymentCreatedTopic() {
        return TopicBuilder.name(paymentTopic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic systemAlertCreatedTopic() {
        return TopicBuilder.name(alertTopic).partitions(1).replicas(1).build();
    }
}