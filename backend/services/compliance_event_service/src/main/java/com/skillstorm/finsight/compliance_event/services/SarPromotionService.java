package com.skillstorm.finsight.compliance_event.services;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillstorm.finsight.compliance_event.models.AuditAction;
import com.skillstorm.finsight.compliance_event.models.AuditActionType;
import com.skillstorm.finsight.compliance_event.models.ComplianceEvent;
import com.skillstorm.finsight.compliance_event.models.ComplianceEventLink;
import com.skillstorm.finsight.compliance_event.models.ComplianceEventSarDetail;
import com.skillstorm.finsight.compliance_event.models.EventStatus;
import com.skillstorm.finsight.compliance_event.models.EventType;
import com.skillstorm.finsight.compliance_event.repositories.AuditActionRepository;
import com.skillstorm.finsight.compliance_event.repositories.ComplianceEventLinkRepository;
import com.skillstorm.finsight.compliance_event.repositories.ComplianceEventRepository;
import com.skillstorm.finsight.compliance_event.repositories.ComplianceEventSarDetailRepository;

@Service
public class SarPromotionService {

    private static final Logger log = LoggerFactory.getLogger(SarPromotionService.class);

    private final ComplianceEventRepository eventRepo;
    private final ComplianceEventSarDetailRepository sarDetailRepo;
    private final ComplianceEventLinkRepository linkRepo;
    private final AuditActionRepository auditRepo;
    private final ObjectMapper objectMapper;

    public SarPromotionService(
            ComplianceEventRepository eventRepo,
            ComplianceEventSarDetailRepository sarDetailRepo,
            ComplianceEventLinkRepository linkRepo,
            AuditActionRepository auditRepo,
            ObjectMapper objectMapper) {
        this.eventRepo = eventRepo;
        this.sarDetailRepo = sarDetailRepo;
        this.linkRepo = linkRepo;
        this.auditRepo = auditRepo;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Optional<ComplianceEvent> promoteFromCtr(ComplianceEvent ctr, CtrSuspicionScoring.ScoreResult score) {

        if (ctr == null || score == null) {
            return Optional.empty();
        }

        int s = score.score();

        if (s >= 60) {
            return Optional.of(createSarForCtrIfAbsent(ctr, score));
        }

        if (s >= 40) {
            flagCtrForAnalystReview(ctr, score);
            return Optional.empty();
        }

        return Optional.empty();
    }

    private ComplianceEvent createSarForCtrIfAbsent(ComplianceEvent ctr, CtrSuspicionScoring.ScoreResult score) {

        String sarIdem = "SAR_FROM_CTR:" + ctr.getEventId();

        var existing = eventRepo.findByIdempotencyKey(sarIdem);
        if (existing.isPresent()) {
            return existing.get();
        }

        // ------------------------
        // CREATE SAR EVENT
        // ------------------------

        ComplianceEvent sar = new ComplianceEvent();
        sar.setEventType(EventType.SAR);
        sar.setSourceSystem("AUTO_FROM_CTR");
        sar.setSourceEntityId("CTR:" + ctr.getEventId());
        sar.setExternalSubjectKey(ctr.getExternalSubjectKey());
        sar.setSourceSubjectType(ctr.getSourceSubjectType());
        sar.setSourceSubjectId(ctr.getSourceSubjectId());
        sar.setEventTime(Instant.now());
        sar.setTotalAmount(ctr.getTotalAmount());
        sar.setStatus(EventStatus.DRAFT);
        sar.setSeverityScore(score.score());
        sar.setCorrelationId(ctr.getCorrelationId());
        sar.setIdempotencyKey(sarIdem);

        ComplianceEvent savedSar = eventRepo.save(sar);

        ComplianceEventSarDetail detail = new ComplianceEventSarDetail();

        detail.setEvent(savedSar);

        Instant activityStart = ctr.getEventTime() != null ? ctr.getEventTime() : Instant.now();
        detail.setActivityStart(activityStart);
        detail.setActivityEnd(activityStart.plus(1, ChronoUnit.DAYS));

        detail.setNarrative(buildNarrative(ctr, score));

        Map<String, Object> form = new HashMap<>();
        form.put("source", "AUTO_FROM_CTR");
        form.put("promotedFromCtrEventId", ctr.getEventId());
        form.put("suspicionScore", score.score());
        form.put("suspicionBand", score.band());
        form.put("suspicionDrivers", score.drivers());

        detail.setFormData(form);

        sarDetailRepo.save(detail);

        // ------------------------
        // LINK CTR -> SAR
        // ------------------------

        ComplianceEventLink link = new ComplianceEventLink(
                ctr,
                savedSar,
                ComplianceEventLink.LinkType.CTR_SUPPORTS_SAR);

        link.setEvidenceSnapshot(objectMapper.convertValue(form, Map.class));
        linkRepo.save(link);

        // ------------------------
        // AUDIT
        // ------------------------

        auditRepo.save(audit(ctr,
                "CTR_PROMOTED_TO_SAR",
                Map.of("sarEventId", savedSar.getEventId(), "score", score.score())));

        auditRepo.save(audit(savedSar,
                "SAR_AUTO_GENERATED",
                Map.of("ctrEventId", ctr.getEventId(), "score", score.score())));

        log.info("Auto-generated SAR eventId={} from CTR eventId={} score={}",
                savedSar.getEventId(), ctr.getEventId(), score.score());

        return savedSar;
    }

    private void flagCtrForAnalystReview(ComplianceEvent ctr, CtrSuspicionScoring.ScoreResult score) {

        String idem = "audit:CTR:" + ctr.getEventId() + ":FLAGGED_FOR_ANALYST_REVIEW";

        if (auditRepo.findByIdempotencyKey(idem).isPresent()) {
            return;
        }

        AuditAction a = audit(ctr, "CTR_FLAGGED_FOR_ANALYST_REVIEW", Map.of(
                "score", score.score(),
                "band", score.band(),
                "drivers", score.drivers()));

        a.setIdempotencyKey(idem);
        auditRepo.save(a);
    }

    private AuditAction audit(ComplianceEvent event, String action, Map<String, Object> metadata) {

        AuditActionType type = toAuditType(action);

        Map<String, Object> meta = new HashMap<>();
        if (metadata != null) {
            meta.putAll(metadata);
        }

        if (type == AuditActionType.SYSTEM_DECISION && action != null && !action.isBlank()) {
            meta.putIfAbsent("action", action);
        }

        AuditAction a = AuditAction.create(event, type, meta);
        a.setActorRole("SYSTEM");
        a.setCorrelationId(event.getCorrelationId());
        a.setIdempotencyKey("audit:" + action + ":" + event.getEventId());

        return a;
    }

    private AuditActionType toAuditType(String action) {
        if (action == null || action.isBlank()) {
            return AuditActionType.SYSTEM_DECISION;
        }
        try {
            return AuditActionType.valueOf(action);
        } catch (IllegalArgumentException ex) {
            return AuditActionType.SYSTEM_DECISION;
        }
    }

    private String buildNarrative(ComplianceEvent ctr, CtrSuspicionScoring.ScoreResult score) {

        String subjectKey = ctr.getExternalSubjectKey() != null
                ? ctr.getExternalSubjectKey()
                : "UNKNOWN_SUBJECT";

        String amount = ctr.getTotalAmount() != null
                ? ctr.getTotalAmount().toPlainString()
                : "0.00";

        return "This SAR was auto-generated from a CTR due to elevated suspicion score. "
                + "SubjectKey=" + subjectKey
                + ", CTRAmount=" + amount
                + ", SuspicionScore=" + score.score()
                + ", Band=" + score.band()
                + ", Drivers=" + String.join(",", score.drivers())
                + ".";
    }
}
