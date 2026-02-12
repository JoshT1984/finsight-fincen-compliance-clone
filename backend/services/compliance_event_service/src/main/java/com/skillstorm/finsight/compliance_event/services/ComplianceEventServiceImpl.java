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
import com.skillstorm.finsight.compliance_event.client.SuspectRegistryClient;
import com.skillstorm.finsight.compliance_event.repositories.ComplianceEventSarDetailRepository;
import com.skillstorm.finsight.compliance_event.repositories.SuspectSnapshotAtTimeOfEventRepository;

@Service
public class ComplianceEventServiceImpl implements ComplianceEventService {

    private final ComplianceEventRepository complianceEventRepository;
    private final ComplianceEventSarDetailRepository sarDetailRepository;
    private final ComplianceEventCtrDetailRepository ctrDetailRepository;
    private final SuspectSnapshotAtTimeOfEventRepository suspectSnapshotRepository;
    private final ComplianceEventMapper mapper;
    private final SuspectRegistryClient suspectRegistryClient;
    private final ComplianceEventEmitter complianceEventEmitter;

    public ComplianceEventServiceImpl(
            ComplianceEventRepository complianceEventRepository,
            ComplianceEventSarDetailRepository sarDetailRepository,
            ComplianceEventCtrDetailRepository ctrDetailRepository,
            SuspectSnapshotAtTimeOfEventRepository suspectSnapshotRepository,
            ComplianceEventMapper mapper,
            @Autowired(required = false) SuspectRegistryClient suspectRegistryClient) {
            ComplianceEventMapper mapper, ComplianceEventEmitter complianceEventEmitter) {

        this.complianceEventRepository = complianceEventRepository;
        this.sarDetailRepository = sarDetailRepository;
        this.ctrDetailRepository = ctrDetailRepository;
        this.suspectSnapshotRepository = suspectSnapshotRepository;
        this.mapper = mapper;
        this.suspectRegistryClient = suspectRegistryClient;
        this.complianceEventEmitter = complianceEventEmitter;
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
        sar.setEventType(EventType.SAR.name()); // Required for @NotNull; DB also has default
        sar.setNarrative(request.narrative());
        sar.setActivityStart(request.activityStart());
        sar.setActivityEnd(request.activityEnd());
        sar.setFormData(request.formData() == null ? Map.of() : request.formData());
        ComplianceEventSarDetail saved = sarDetailRepository.save(sar);

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

        tryLinkEventToSuspectBySsn(event, request.externalSubjectKey(), null);
        if (event.getSuspectSnapshot() != null && request.formData() != null) {
            pushParsedAddressToSuspect(event.getSuspectSnapshot().getSuspectId(), request.formData());
        }
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
        ctr.setEventType(EventType.CTR.name()); // Required for @NotNull; DB also has default
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

        tryLinkEventToSuspectBySsn(event, request.externalSubjectKey(), request.customerName());
        if (event.getSuspectSnapshot() != null && request.ctrFormData() != null) {
            pushParsedAddressToSuspect(event.getSuspectSnapshot().getSuspectId(), request.ctrFormData());
        }
        return mapper.toResponse(event);
    }

    /**
     * If externalSubjectKey is SSN:xxxxxxxxx, finds or creates a suspect by SSN (and name when provided),
     * then links the event to that suspect. When the form name differs from the suspect's primary name,
     * the suspect registry adds it as an alias (AKA).
     */
    private void tryLinkEventToSuspectBySsn(ComplianceEvent event, String externalSubjectKey, String primaryName) {
        if (suspectRegistryClient == null) return;
        String ssn = SuspectRegistryClient.extractSsnFromSubjectKey(externalSubjectKey);
        if (ssn == null) return;
        suspectRegistryClient.findOrCreateBySsnAndName(ssn, primaryName).ifPresent(suspectId -> {
            SuspectSnapshotAtTimeOfEvent snapshot = suspectSnapshotRepository.findLatestForSuspect(suspectId);
            if (snapshot == null) {
                snapshot = new SuspectSnapshotAtTimeOfEvent(suspectId);
                snapshot = suspectSnapshotRepository.save(snapshot);
            }
            event.setSuspectSnapshot(snapshot);
            complianceEventRepository.save(event);
        });
    }

    /** Pushes parsed CTR/SAR form address to the suspect registry when present in form data. */
    private void pushParsedAddressToSuspect(long suspectId, Map<String, Object> formData) {
        if (suspectRegistryClient == null || formData == null) return;
        String line1 = getString(formData, "_parsedAddressLine1");
        String city = getString(formData, "_parsedAddressCity");
        String country = getString(formData, "_parsedAddressCountry");
        if (line1 == null || city == null || country == null) return;
        String line2 = getString(formData, "_parsedAddressLine2");
        String state = getString(formData, "_parsedAddressState");
        String postalCode = getString(formData, "_parsedAddressPostalCode");
        suspectRegistryClient.ensureAddressForSuspect(suspectId, line1, line2, city, state, postalCode, country);
    }

    private static String getString(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString().trim() : null;
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
                .filter(e -> !e.getEventId().equals(eventId)) // change to getEventId() if needed
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

        // Seed SAR formData with CTR-derived info (can be expanded to match FIN-109
        // fields)
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
                    .map(v -> v instanceof Number n ? n.longValue() : tryParseLong(v))
                    .filter(v -> v != null)
                    .toList();
        }

        return List.of();
    }

    private Long tryParseLong(Object v) {
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (Exception ex) {
            return null;
        }
    }

    private void assertNoDuplicateSource(String sourceSystem, String sourceEntityId) {
        boolean exists = complianceEventRepository
                .findBySourceSystemAndSourceEntityId(sourceSystem, sourceEntityId, PageRequest.of(0, 1))
                .hasContent();

        if (exists) {
            throw new ResourceConflictException("Compliance event already exists for sourceSystem + sourceEntityId");
        }
    }
}
