package com.skillstorm.finsight.compliance_event.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import com.skillstorm.finsight.compliance_event.security.ServiceJwtTokenProvider;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(ServiceJwtTokenProvider tokenProvider) {
        RestTemplate rt = new RestTemplate();
        rt.getInterceptors().add((req, body, ex) -> {
            // only add if not already present
            if (!req.getHeaders().containsKey("Authorization")) {
                req.getHeaders().setBearerAuth(tokenProvider.getToken());
            }
            return ex.execute(req, body);
        });
        return rt;
    }
}
