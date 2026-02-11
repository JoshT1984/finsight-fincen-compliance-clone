package com.skillstorm.finsight.compliance_event.client;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Client for suspect-registry-service to look up suspect by SSN (for auto-linking
 * CTR/SAR events when externalSubjectKey is SSN:xxxxxxxxx).
 */
@Component
@ConditionalOnProperty(name = "finsight.suspect-registry.url")
public class SuspectRegistryClient {

    private static final Logger log = LoggerFactory.getLogger(SuspectRegistryClient.class);
    private static final String SSN_PREFIX = "SSN:";

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public SuspectRegistryClient(
            RestTemplate restTemplate,
            @Value("${finsight.suspect-registry.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /**
     * Returns externalSubjectKey if it looks like SSN:xxxxxxxxx, else null.
     */
    public static String extractSsnFromSubjectKey(String externalSubjectKey) {
        if (externalSubjectKey == null || !externalSubjectKey.startsWith(SSN_PREFIX)) return null;
        String digits = externalSubjectKey.substring(SSN_PREFIX.length()).replaceAll("[^0-9]", "");
        return digits.length() == 9 ? digits : null;
    }

    /**
     * Looks up suspect ID by SSN. Returns empty if URL not set, SSN invalid, or no match.
     */
    public Optional<Long> findSuspectIdBySsn(String ssn) {
        if (ssn == null || ssn.isBlank()) return Optional.empty();
        String url = baseUrl + "/api/suspects/by-ssn?ssn=" + ssn.replace(" ", "");
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object id = response.getBody().get("suspectId");
                if (id instanceof Number) {
                    return Optional.of(((Number) id).longValue());
                }
            }
        } catch (Exception e) {
            log.debug("Suspect registry lookup by SSN failed: {}", e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Finds suspect by SSN or creates one with the given name. Returns empty if call fails.
     */
    public Optional<Long> findOrCreateBySsnAndName(String ssn, String primaryName) {
        if (ssn == null || ssn.isBlank()) return Optional.empty();
        String url = baseUrl + "/api/suspects/find-or-create";
        try {
            Map<String, String> body = new java.util.HashMap<>();
            body.put("ssn", ssn.replace(" ", ""));
            body.put("primaryName", primaryName != null ? primaryName : "Unknown");
            ResponseEntity<Map> response = restTemplate.postForEntity(url, body, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object id = response.getBody().get("suspectId");
                if (id instanceof Number) {
                    return Optional.of(((Number) id).longValue());
                }
            }
        } catch (Exception e) {
            log.debug("Suspect registry find-or-create failed: {}", e.getMessage());
        }
        return Optional.empty();
    }
}
