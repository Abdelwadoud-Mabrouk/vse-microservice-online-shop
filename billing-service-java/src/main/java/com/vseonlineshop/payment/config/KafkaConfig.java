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

    @Value("${kafka.topics.alerts.system}")
    private String alertSystemTopic;

    @Value("${kafka.topics.alerts.security}")
    private String alertSycurityTopic;

    @Bean
    public NewTopic paymentCreatedTopic() {
        return TopicBuilder.name(paymentTopic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic systemAlertCreatedTopic() {
        return TopicBuilder.name(alertSystemTopic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic sycurityAlertCreatedTopic() {
        return TopicBuilder.name(alertSycurityTopic).partitions(1).replicas(1).build();
    }
}