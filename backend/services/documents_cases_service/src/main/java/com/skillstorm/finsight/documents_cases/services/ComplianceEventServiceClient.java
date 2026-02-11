package com.skillstorm.finsight.documents_cases.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.skillstorm.finsight.documents_cases.dtos.compliance.ComplianceEventResponsePayload;
import com.skillstorm.finsight.documents_cases.dtos.compliance.CreateCtrRequestPayload;
import com.skillstorm.finsight.documents_cases.dtos.compliance.CreateSarRequestPayload;

@Service
public class ComplianceEventServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ComplianceEventServiceClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ComplianceEventServiceClient(
            @Qualifier("complianceEventRestTemplate") RestTemplate restTemplate,
            @Value("${compliance-event-service.url:http://localhost:8085}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /**
     * Creates a CTR record in the compliance-event-service.
     *
     * @param request The CTR payload (from PDF extraction or minimal defaults).
     * @return The created event ID (use as ctrId).
     */
    public Long createCtr(CreateCtrRequestPayload request) {
        String url = baseUrl + "/api/compliance-events/ctr";
        log.debug("Creating CTR via {}", url);
        try {
            ComplianceEventResponsePayload response = restTemplate.postForObject(url, request, ComplianceEventResponsePayload.class);
            if (response == null || response.eventId() == null) {
                throw new IllegalStateException("Compliance service returned no event ID for CTR");
            }
            log.info("Created CTR with eventId: {}", response.eventId());
            return response.eventId();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                throw new IllegalArgumentException("CTR already exists for this source (duplicate sourceSystem/sourceEntityId): " + e.getMessage(), e);
            }
            log.error("Compliance service error creating CTR: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalStateException("Failed to create CTR: " + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            String body = e.getResponseBodyAsString();
            log.error("Compliance service 5xx creating CTR: {} {}", e.getStatusCode(), body);
            String detail = parseDetailFromProblemDetail(body);
            throw new IllegalStateException(detail != null ? detail : "Compliance service error (500). Check compliance service logs.", e);
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("Cannot reach compliance-event-service at {}: {}", baseUrl, e.getMessage());
            throw new IllegalStateException("Compliance service unavailable at " + baseUrl + ". " + e.getMessage(), e);
        }
    }

    /**
     * Creates a SAR record in the compliance-event-service.
     *
     * @param request The SAR payload (from PDF extraction or minimal defaults).
     * @return The created event ID (use as sarId).
     */
    public Long createSar(CreateSarRequestPayload request) {
        String url = baseUrl + "/api/compliance-events/sar";
        log.debug("Creating SAR via {}", url);
        try {
            ComplianceEventResponsePayload response = restTemplate.postForObject(url, request, ComplianceEventResponsePayload.class);
            if (response == null || response.eventId() == null) {
                throw new IllegalStateException("Compliance service returned no event ID for SAR");
            }
            log.info("Created SAR with eventId: {}", response.eventId());
            return response.eventId();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                throw new IllegalArgumentException("SAR already exists for this source (duplicate sourceSystem/sourceEntityId): " + e.getMessage(), e);
            }
            log.error("Compliance service error creating SAR: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalStateException("Failed to create SAR: " + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            String body = e.getResponseBodyAsString();
            log.error("Compliance service 5xx creating SAR: {} {}", e.getStatusCode(), body);
            String detail = parseDetailFromProblemDetail(body);
            throw new IllegalStateException(detail != null ? detail : "Compliance service error (500). Check compliance service logs.", e);
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("Cannot reach compliance-event-service at {}: {}", baseUrl, e.getMessage());
            throw new IllegalStateException("Compliance service unavailable at " + baseUrl + ". " + e.getMessage(), e);
        }
    }

    private static String parseDetailFromProblemDetail(String jsonBody) {
        if (jsonBody == null || jsonBody.isBlank()) return null;
        int start = jsonBody.indexOf("\"detail\":\"");
        if (start < 0) return null;
        start += 10;
        int end = jsonBody.indexOf("\"", start);
        if (end < 0) return null;
        return jsonBody.substring(start, end).replace("\\\"", "\"");
    }
}
