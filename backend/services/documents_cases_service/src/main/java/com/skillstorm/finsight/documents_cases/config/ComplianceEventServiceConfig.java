package com.skillstorm.finsight.documents_cases.config;

import java.time.Duration;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import com.skillstorm.finsight.documents_cases.utils.SecurityContextUtils;

@Configuration
public class ComplianceEventServiceConfig {

    /**
     * Interceptor that forwards the current user's JWT to the compliance service so
     * the request is authorized (same user context as the upload request).
     */
    private static final ClientHttpRequestInterceptor BEARER_PROPAGATION = (HttpRequest request, byte[] body,
            ClientHttpRequestExecution execution) -> {
        SecurityContextUtils.getCurrentBearerToken()
                .ifPresent(token -> request.getHeaders().set("Authorization", "Bearer " + token));
        return execution.execute(request, body);
    };

    @Bean
    public RestTemplate complianceEventRestTemplate(
            RestTemplateBuilder builder,
            @Value("${compliance-event-service.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${compliance-event-service.read-timeout-ms:30000}") int readTimeoutMs) {
        RestTemplate restTemplate = builder
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(Duration.ofMillis(readTimeoutMs))
                .build();
        restTemplate.setInterceptors(Collections.singletonList(BEARER_PROPAGATION));
        return restTemplate;
    }
}
