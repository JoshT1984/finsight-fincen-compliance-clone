package com.skillstorm.finsight.compliance_event.client;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@ConditionalOnProperty(name = "finsight.documents-cases.url")
public class DocumentsCasesClient {

    private static final Logger log = LoggerFactory.getLogger(DocumentsCasesClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public DocumentsCasesClient(
            RestTemplate restTemplate,
            @Value("${finsight.documents-cases.url}") String baseUrl) {

        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;

        log.info("DocumentsCasesClient initialized with baseUrl={}", this.baseUrl);
    }

    public void ensureCaseForSar(long sarEventId) {
        log.info("Ensuring SAR case for sarEventId={}", sarEventId);
        postIdempotent(Map.of("sarId", sarEventId, "status", "OPEN"), "SAR", sarEventId);
    }

    public void ensureCaseForCtrAnalystReview(long ctrEventId) {
        log.info("Ensuring CTR review case for ctrEventId={}", ctrEventId);
        postIdempotent(Map.of("ctrId", ctrEventId, "status", "OPEN"), "CTR", ctrEventId);
    }

    /**
     * IMPORTANT:
     * This method is now NON-FATAL.
     * Case creation failures will NOT throw and will NOT roll back CTR creation.
     */
    private void postIdempotent(Map<String, Object> body, String type, long id) {

        String url = baseUrl + "/api/cases";

        log.info("POST {} body={}", url, body);

        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            log.info("Case service response status={} for {} {}",
                    response.getStatusCode(), type, id);

        } catch (HttpClientErrorException e) {

            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                log.info("CaseFile already exists for {} {} (409)", type, id);
                return;
            }

            // 🔥 DO NOT THROW — log and continue
            log.warn("Case service returned error for {} {} - status={} body={}",
                    type,
                    id,
                    e.getStatusCode(),
                    e.getResponseBodyAsString());

        } catch (RestClientException e) {

            // Network failures, service down, etc.
            log.warn("Case service unavailable for {} {} - reason={}",
                    type,
                    id,
                    e.getMessage());

        }
    }
}