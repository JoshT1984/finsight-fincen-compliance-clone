package com.skillstorm.finsight.compliance_event.services;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.skillstorm.finsight.compliance_event.client.DocumentsCasesClient;
import com.skillstorm.finsight.compliance_event.dtos.ComplianceEventResponse;
import com.skillstorm.finsight.compliance_event.dtos.CreateCtrRequest;
import com.skillstorm.finsight.compliance_event.dtos.CreateSarRequest;
import com.skillstorm.finsight.compliance_event.dtos.CtrDetailResponse;
import com.skillstorm.finsight.compliance_event.emitters.ComplianceEventEmitter;
import com.skillstorm.finsight.compliance_event.exceptions.ResourceConflictException;
import com.skillstorm.finsight.compliance_event.exceptions.ResourceNotFoundException;
import com.skillstorm.finsight.compliance_event.loggers.ComplianceEventLog;
import com.skillstorm.finsight.compliance_event.mappers.ComplianceEventMapper;
import com.skillstorm.finsight.compliance_event.models.ComplianceEvent;
import com.skillstorm.finsight.compliance_event.models.ComplianceEventCtrDetail;
import com.skillstorm.finsight.compliance_event.models.ComplianceEventSarDetail;
import com.skillstorm.finsight.compliance_event.models.EventStatus;
import com.skillstorm.finsight.compliance_event.models.EventType;
import com.skillstorm.finsight.compliance_event.models.SuspectSnapshotAtTimeOfEvent;
import com.skillstorm.finsight.compliance_event.repositories.ComplianceEventCtrDetailRepository;
import com.skillstorm.finsight.compliance_event.repositories.ComplianceEventRepository;
import com.skillstorm.finsight.compliance_event.repositories.ComplianceEventSarDetailRepository;
import com.skillstorm.finsight.compliance_event.repositories.SuspectSnapshotAtTimeOfEventRepository;

@Service
public class ComplianceEventServiceImpl implements ComplianceEventService {

    private final ComplianceEventRepository complianceEventRepository;
    private final ComplianceEventSarDetailRepository sarDetailRepository;
    private final ComplianceEventCtrDetailRepository ctrDetailRepository;
    private final SuspectSnapshotAtTimeOfEventRepository suspectSnapshotRepository;
    private final ComplianceEventMapper mapper;
    private final ComplianceEventEmitter complianceEventEmitter;
    private final DocumentsCasesClient documentsCasesClient;

    public ComplianceEventServiceImpl(
            ComplianceEventRepository complianceEventRepository,
            ComplianceEventSarDetailRepository sarDetailRepository,
            ComplianceEventCtrDetailRepository ctrDetailRepository,
            SuspectSnapshotAtTimeOfEventRepository suspectSnapshotRepository,
            ComplianceEventMapper mapper,
            ComplianceEventEmitter complianceEventEmitter,
            DocumentsCasesClient documentsCasesClient) {

        this.complianceEventRepository = complianceEventRepository;
        this.sarDetailRepository = sarDetailRepository;
        this.ctrDetailRepository = ctrDetailRepository;
        this.suspectSnapshotRepository = suspectSnapshotRepository;
        this.mapper = mapper;
        this.complianceEventEmitter = complianceEventEmitter;
        this.documentsCasesClient = documentsCasesClient;
    }

    /**
     * Run the provided action after the surrounding @Transactional method
     * successfully commits.
     * This prevents creating a CaseFile for a SAR that later rolls back.
     */
    private void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    @Override
    @Transactional
    public ComplianceEventResponse createSar(CreateSarRequest request) {
        assertNoDuplicateSource(request.sourceSystem(), request.sourceEntityId());

        ComplianceEvent event = new ComplianceEvent();
        event.setEventType(EventType.SAR);
        event.setSourceSystem(request.sourceSystem());
        event.setSourceEntityId(request.sourceEntityId());
        event.setExternalSubjectKey(request.externalSubjectKey());
        event.setEventTime(request.eventTime());
        event.setTotalAmount(request.totalAmount());
        event.setSeverityScore(request.severityScore());

        // SAR status must be DRAFT or SUBMITTED
        event.setStatus(EventStatus.DRAFT);

        try {
            event = complianceEventRepository.save(event);
        } catch (DataIntegrityViolationException ex) {
            throw new ResourceConflictException("Compliance event already exists for sourceSystem + sourceEntityId");
        }

        ComplianceEventSarDetail sar = new ComplianceEventSarDetail();
        sar.setEvent(event);
        sar.setNarrative(request.narrative());
        sar.setActivityStart(request.activityStart());
        sar.setActivityEnd(request.activityEnd());
        sar.setFormData(request.formData() == null ? Map.of() : request.formData());
        ComplianceEventSarDetail saved = sarDetailRepository.save(sar);

        // Ensure a case file is created for this SAR AFTER the SAR transaction commits.
        // This avoids creating an orphaned case if the SAR creation rolls back.
        runAfterCommit(() -> {
            try {
                if (documentsCasesClient != null) {
                    documentsCasesClient.ensureCaseForSar(saved.getEventId());
                }
            } catch (Exception ex) {
                // Don't crash the request after commit; log so it can be retried/admin-created.
                org.slf4j.LoggerFactory.getLogger(ComplianceEventServiceImpl.class)
                        .warn("Failed to auto-create CaseFile for SAR {}: {}", saved.getEventId(), ex.getMessage());
            }
        });

        // Emitting event log for SAR creation
        String trigger = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        complianceEventEmitter.emit(new ComplianceEventLog(
                Instant.now(),
                "COMPLIANCE_EVENT",
                saved.getEventId().toString(),
                "CREATED",
                trigger.isBlank() ? "UNKNOWN" : trigger,
                "SAR_CREATED",
                "SAR_CREATED:" + saved.getEventId(),
                Map.of("eventType", "SAR", "sourceSystem", request.sourceSystem())));

        return mapper.toResponse(event);
    }

    @Override
    @Transactional
    public ComplianceEventResponse createCtr(CreateCtrRequest request) {
        assertNoDuplicateSource(request.sourceSystem(), request.sourceEntityId());

        ComplianceEvent event = new ComplianceEvent();
        event.setEventType(EventType.CTR);
        event.setSourceSystem(request.sourceSystem());
        event.setSourceEntityId(request.sourceEntityId());
        event.setExternalSubjectKey(request.externalSubjectKey());
        event.setEventTime(request.eventTime());
        event.setTotalAmount(request.totalAmount());

        // CTR status must be CREATED or FILED
        event.setStatus(EventStatus.CREATED);

        try {
            event = complianceEventRepository.save(event);
        } catch (DataIntegrityViolationException ex) {
            throw new ResourceConflictException("Compliance event already exists for sourceSystem + sourceEntityId");
        }

        ComplianceEventCtrDetail ctr = new ComplianceEventCtrDetail();
        ctr.setEvent(event);
        ctr.setCustomerName(request.customerName());
        ctr.setTransactionTime(request.transactionTime());
        ctr.setCtrFormData(request.ctrFormData() == null ? Map.of() : request.ctrFormData());
        ComplianceEventCtrDetail saved = ctrDetailRepository.save(ctr);

        // Emitting event log for CTR creation
        String trigger = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        complianceEventEmitter.emit(new ComplianceEventLog(
                Instant.now(),
                "COMPLIANCE_EVENT",
                saved.getEventId().toString(),
                "CREATED",
                trigger.isBlank() ? "UNKNOWN" : trigger,
                "CTR_CREATED",
                "CTR_CREATED:" + saved.getEventId(),
                Map.of("eventType", "CTR", "sourceSystem", request.sourceSystem())));

        return mapper.toResponse(event);
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceEventResponse getById(Long eventId) {
        ComplianceEvent event = complianceEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance event not found: " + eventId));
        return mapper.toResponse(event);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ComplianceEventResponse> search(
            EventType eventType,
            EventStatus status,
            String sourceSystem,
            Instant from,
            Instant to,
            Pageable pageable) {

        return complianceEventRepository
                .search(eventType, status, sourceSystem, from, to, pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ComplianceEventResponse> findBySuspectId(Long suspectId, Pageable pageable) {
        return complianceEventRepository
                .findBySuspectSnapshot_SuspectId(suspectId, pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ComplianceEventResponse> findLinkableByEventType(
            EventType eventType,
            Long excludeSuspectId,
            Pageable pageable) {

        return complianceEventRepository
                .findLinkableByEventType(eventType, excludeSuspectId, pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional
    public ComplianceEventResponse linkEventToSuspect(Long eventId, Long suspectId) {
        ComplianceEvent event = complianceEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance event not found: " + eventId));

        SuspectSnapshotAtTimeOfEvent snapshot = suspectSnapshotRepository.findLatestForSuspect(suspectId);
        if (snapshot == null) {
            snapshot = new SuspectSnapshotAtTimeOfEvent(suspectId);
            snapshot = suspectSnapshotRepository.save(snapshot);
        }

        event.setSuspectSnapshot(snapshot);
        ComplianceEvent saved = complianceEventRepository.save(event);

        // Emitting event log for linking event to suspect
        String trigger = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        complianceEventEmitter.emit(new ComplianceEventLog(
                Instant.now(),
                "COMPLIANCE_EVENT",
                saved.getEventId().toString(),
                "UPDATED",
                trigger.isBlank() ? "UNKNOWN" : trigger,
                "EVENT_LINKED_TO_SUSPECT",
                "EVENT_LINKED_TO_SUSPECT:" + saved.getEventId(),
                Map.of("suspectId", snapshot.getSuspectId())));

        return mapper.toResponse(event);
    }

    @Override
    @Transactional
    public ComplianceEventResponse unlinkEventFromSuspect(Long eventId) {
        ComplianceEvent event = complianceEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance event not found: " + eventId));
        event.setSuspectSnapshot(null);
        ComplianceEvent saved = complianceEventRepository.save(event);

        // Emitting event log for delinking event to suspect
        String trigger = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        complianceEventEmitter.emit(new ComplianceEventLog(
                Instant.now(),
                "COMPLIANCE_EVENT",
                saved.getEventId().toString(),
                "UPDATED",
                trigger.isBlank() ? "UNKNOWN" : trigger,
                "EVENT_DELINKED_FROM_SUSPECT",
                "EVENT_DELINKED_FROM_SUSPECT:" + saved.getEventId(),
                Map.of()));

        return mapper.toResponse(event);
    }

    @Override
    @Transactional(readOnly = true)
    public CtrDetailResponse getCtrDetail(Long eventId) {
        ComplianceEvent event = complianceEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance event not found: " + eventId));

        if (event.getEventType() != EventType.CTR) {
            throw new ResourceConflictException("Event is not a CTR: " + eventId);
        }

        ComplianceEventCtrDetail detail = ctrDetailRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("CTR detail not found for event: " + eventId));

        Map<String, Object> ctrFormData = detail.getCtrFormData() == null ? Map.of() : detail.getCtrFormData();

        List<Long> contributingTxnIds = extractTxnIds(ctrFormData);

        List<ComplianceEventResponse> priorCtrs = complianceEventRepository
                .findByExternalSubjectKeyAndEventTypeOrderByEventTimeDesc(
                        event.getExternalSubjectKey(),
                        EventType.CTR)
                .stream()
                .filter(e -> !e.getEventId().equals(eventId))
                .map(mapper::toResponse)
                .toList();

        return new CtrDetailResponse(
                mapper.toResponse(event),
                ctrFormData,
                contributingTxnIds,
                priorCtrs);
    }

    @Override
    @Transactional
    public ComplianceEventResponse generateSarFromCtr(Long ctrEventId) {
        ComplianceEvent ctrEvent = complianceEventRepository.findById(ctrEventId)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance event not found: " + ctrEventId));

        if (ctrEvent.getEventType() != EventType.CTR) {
            throw new ResourceConflictException("Event is not a CTR: " + ctrEventId);
        }

        ComplianceEventCtrDetail ctrDetail = ctrDetailRepository.findById(ctrEventId)
                .orElseThrow(() -> new ResourceNotFoundException("CTR detail not found for event: " + ctrEventId));

        // Idempotent source identifiers for the SAR draft derived from this CTR
        String sourceSystem = "AUTO_FROM_CTR";
        String sourceEntityId = "CTR:" + ctrEventId;

        assertNoDuplicateSource(sourceSystem, sourceEntityId);

        ComplianceEvent sarEvent = new ComplianceEvent();
        sarEvent.setEventType(EventType.SAR);
        sarEvent.setSourceSystem(sourceSystem);
        sarEvent.setSourceEntityId(sourceEntityId);
        sarEvent.setExternalSubjectKey(ctrEvent.getExternalSubjectKey());
        sarEvent.setEventTime(Instant.now());
        sarEvent.setTotalAmount(ctrEvent.getTotalAmount());
        sarEvent.setSeverityScore(ctrEvent.getSeverityScore());
        sarEvent.setStatus(EventStatus.DRAFT);

        sarEvent = complianceEventRepository.save(sarEvent);

        Map<String, Object> ctrFormData = ctrDetail.getCtrFormData() == null ? Map.of() : ctrDetail.getCtrFormData();

        String subjectKey = String.valueOf(ctrFormData.getOrDefault("subjectKey", ctrEvent.getExternalSubjectKey()));

        Object driversObj = ctrFormData.get("suspicionDrivers");
        String drivers = "";
        if (driversObj instanceof List<?> list && !list.isEmpty()) {
            drivers = list.stream().map(String::valueOf).reduce((a, b) -> a + ", " + b).orElse("");
        }

        String narrative = "Auto-generated SAR draft from CTR " + ctrEventId +
                " for subject " + subjectKey +
                ". Suspicion score: " + (ctrEvent.getSeverityScore() == null ? "—" : ctrEvent.getSeverityScore()) +
                (drivers.isBlank() ? "" : ". Drivers: " + drivers) +
                ". Please review and edit before submission.";

        ComplianceEventSarDetail sarDetail = new ComplianceEventSarDetail();
        sarDetail.setEvent(sarEvent);
        sarDetail.setNarrative(narrative);
        // Use CTR day as activity window by default
        sarDetail.setActivityStart(ctrEvent.getEventTime());
        sarDetail.setActivityEnd(ctrEvent.getEventTime());

        // Seed SAR formData with CTR-derived info
        Map<String, Object> seed = new java.util.HashMap<>();
        seed.put("fromCtrEventId", ctrEventId);
        seed.put("subjectKey", subjectKey);
        seed.put("totalCashAmount", ctrFormData.getOrDefault("totalCashAmount", null));
        seed.put("txnDay", ctrFormData.getOrDefault("txnDay", null));
        seed.put("suspicionScore", ctrFormData.getOrDefault("suspicionScore", ctrEvent.getSeverityScore()));
        seed.put("suspicionBand", ctrFormData.getOrDefault("suspicionBand", null));
        seed.put("suspicionDrivers", ctrFormData.getOrDefault("suspicionDrivers", List.of()));
        seed.put("contributingTxnIds", ctrFormData.getOrDefault("contributingTxnIds", List.of()));
        sarDetail.setFormData(seed);

        ComplianceEventSarDetail saved = sarDetailRepository.save(sarDetail);

        // Ensure a case file is created for this auto-generated SAR AFTER the
        // transaction commits.
        runAfterCommit(() -> {
            try {
                if (documentsCasesClient != null) {
                    documentsCasesClient.ensureCaseForSar(saved.getEventId());
                }
            } catch (Exception ex) {
                org.slf4j.LoggerFactory.getLogger(ComplianceEventServiceImpl.class)
                        .warn("Failed to auto-create CaseFile for auto-generated SAR {}: {}", saved.getEventId(),
                                ex.getMessage());
            }
        });

        // Emitting event log for SAR creation
        complianceEventEmitter.emit(new ComplianceEventLog(
                Instant.now(),
                "COMPLIANCE_EVENT",
                saved.getEventId().toString(),
                "CREATED",
                "SYSTEM",
                "SAR_AUTO_GENERATED_FROM_CTR",
                "SAR_AUTO_GENERATED_FROM_CTR:" + saved.getEventId(),
                Map.of("eventType", "SAR", "fromCtrEventId", ctrEventId, "sourceSystem", "AUTO_FROM_CTR")));

        return mapper.toResponse(sarEvent);
    }

    @SuppressWarnings("unchecked")
    private List<Long> extractTxnIds(Map<String, Object> ctrFormData) {
        Object raw = ctrFormData.get("contributingTxnIds");
        if (raw == null)
            return List.of();
        if (raw instanceof List<?> list) {
            return list.stream()
                    .map(v -> {
                        if (v instanceof Number n)
                            return n.longValue();
                        try {
                            return Long.parseLong(String.valueOf(v));
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(v -> v != null)
                    .toList();
        }
        return List.of();
    }

    private void assertNoDuplicateSource(String sourceSystem, String sourceEntityId) {
        if (sourceSystem == null || sourceEntityId == null)
            return;

        var page = complianceEventRepository.findBySourceSystemAndSourceEntityId(
                sourceSystem,
                sourceEntityId,
                PageRequest.of(0, 1));

        if (page != null && page.hasContent()) {
            throw new ResourceConflictException("Compliance event already exists for sourceSystem + sourceEntityId");
        }
    }

}
