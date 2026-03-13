package com.skillstorm.finsight.compliance_event.services;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillstorm.finsight.compliance_event.models.ComplianceEvent;
import com.skillstorm.finsight.compliance_event.models.ComplianceEventCtrDetail;
import com.skillstorm.finsight.compliance_event.repositories.ComplianceEventCtrDetailRepository;
import com.skillstorm.finsight.compliance_event.repositories.ComplianceEventRepository;

@Service
public class SarPromotionService {

    private static final Logger log = LoggerFactory.getLogger(SarPromotionService.class);

    // LOWERED thresholds (was 60/40)
    private static final int CREATE_SAR_THRESHOLD = 50;
    private static final int FLAG_FOR_REVIEW_THRESHOLD = 30;

    private final ComplianceEventRepository eventRepo;
    private final ComplianceEventCtrDetailRepository ctrDetailRepo;
    private final ComplianceEventService complianceEventService;

    public SarPromotionService(
            ComplianceEventRepository eventRepo,
            ComplianceEventCtrDetailRepository ctrDetailRepo,
            ComplianceEventService complianceEventService) {
        this.eventRepo = eventRepo;
        this.ctrDetailRepo = ctrDetailRepo;
        this.complianceEventService = complianceEventService;
    }

    @Transactional
    public void promoteFromCtr(ComplianceEvent ctrEvent, CtrSuspicionScoring.ScoreResult score) {
        int s = score.score();

        if (s >= CREATE_SAR_THRESHOLD) {
            log.info("Auto-promoting CTR {} to SAR (score={}, band={}, drivers={})",
                    ctrEvent.getEventId(), s, score.band(), score.drivers());
            complianceEventService.generateSarFromCtr(ctrEvent.getEventId());
            return;
        }

        if (s >= FLAG_FOR_REVIEW_THRESHOLD) {
            log.info("CTR {} flagged for review (score={}, band={}, drivers={})",
                    ctrEvent.getEventId(), s, score.band(), score.drivers());
            markCtrForReview(ctrEvent.getEventId(), score);
        }
    }

    private void markCtrForReview(Long ctrEventId, CtrSuspicionScoring.ScoreResult score) {

        // Load CTR detail (where the JSON lives)
        ComplianceEventCtrDetail detail = ctrDetailRepo.findByEventId(ctrEventId)
                .orElseThrow(() -> new IllegalStateException(
                        "CTR detail not found for eventId=" + ctrEventId));

        Map<String, Object> form = detail.getCtrFormData();
        if (form == null) {
            form = new HashMap<>();
        } else {
            // Ensure mutable map
            form = new HashMap<>(form);
        }

        form.put("reviewFlag", true);
        form.put("reviewReason", "Suspicion score exceeded review threshold");
        form.put("suspicionScore", score.score());
        form.put("suspicionBand", score.band());
        form.put("suspicionDrivers", score.drivers());
        form.put("reviewFlaggedAt", Instant.now().toString());

        detail.setCtrFormData(form);
        ctrDetailRepo.save(detail);

        // Optional: touch the event row to ensure transaction persists cleanly / audit
        // hooks fire
        eventRepo.findById(ctrEventId).ifPresent(eventRepo::save);
    }
}
