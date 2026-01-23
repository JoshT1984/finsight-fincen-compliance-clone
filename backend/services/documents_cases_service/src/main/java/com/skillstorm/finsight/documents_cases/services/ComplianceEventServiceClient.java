package com.skillstorm.finsight.documents_cases.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Client for communicating with the compliance-event-service.
 * This service manages CTR and SAR records.
 * 
 * TODO: Implement REST client to call compliance-event-service endpoints
 * when they become available. For now, this is a placeholder.
 */
@Service
public class ComplianceEventServiceClient {
    
    private static final Logger log = LoggerFactory.getLogger(ComplianceEventServiceClient.class);
    
    /**
     * Creates a CTR record in the compliance-event-service.
     * 
     * @return The created CTR ID
     * @throws UnsupportedOperationException If the service is not yet implemented
     */
    public Long createCtrRecord() {
        log.warn("CTR record creation not yet implemented - compliance-event-service endpoints not available");
        throw new UnsupportedOperationException(
            "CTR record creation is not yet implemented. " +
            "Please provide a ctrId when uploading CTR documents, or implement the compliance-event-service REST client."
        );
    }
    
    /**
     * Creates a SAR record in the compliance-event-service.
     * 
     * @return The created SAR ID
     * @throws UnsupportedOperationException If the service is not yet implemented
     */
    public Long createSarRecord() {
        log.warn("SAR record creation not yet implemented - compliance-event-service endpoints not available");
        throw new UnsupportedOperationException(
            "SAR record creation is not yet implemented. " +
            "Please provide a sarId when uploading SAR documents, or implement the compliance-event-service REST client."
        );
    }
}
