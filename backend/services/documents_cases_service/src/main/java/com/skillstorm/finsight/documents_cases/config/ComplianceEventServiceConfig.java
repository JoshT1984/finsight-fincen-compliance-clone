package com.skillstorm.finsight.documents_cases.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class ComplianceEventServiceConfig {

    @Bean
    public RestTemplate complianceEventRestTemplate(
            RestTemplateBuilder builder,
            @Value("${compliance-event-service.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${compliance-event-service.read-timeout-ms:30000}") int readTimeoutMs) {
        return builder
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(Duration.ofMillis(readTimeoutMs))
                .build();
    }
}
