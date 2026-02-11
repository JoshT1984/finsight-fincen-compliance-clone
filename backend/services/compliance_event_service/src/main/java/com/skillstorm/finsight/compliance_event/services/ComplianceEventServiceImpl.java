package com.skillstorm.finsight.compliance_event.services;

import java.time.Instant;
import java.util.Map;

import com.skillstorm.finsight.compliance_event.dtos.ComplianceEventResponse;
import com.skillstorm.finsight.compliance_event.dtos.CreateCtrRequest;
import com.skillstorm.finsight.compliance_event.dtos.CreateSarRequest;
import com.skillstorm.finsight.compliance_event.exceptions.ResourceConflictException;
import com.skillstorm.finsight.compliance_event.exceptions.ResourceNotFoundException;
import com.skillstorm.finsight.compliance_event.mappers.ComplianceEventMapper;
import com.skillstorm.finsight.compliance_event.models.ComplianceEvent;
import com.skillstorm.finsight.compliance_event.models.ComplianceEventCtrDetail;
import com.skillstorm.finsight.compliance_event.models.ComplianceEventSarDetail;
import com.skillstorm.finsight.compliance_event.models.EventStatus;
import com.skillstorm.finsight.compliance_event.models.EventType;
import com.skillstorm.finsight.compliance_event.repositories.ComplianceEventCtrDetailRepository;
import com.skillstorm.finsight.compliance_event.repositories.ComplianceEventRepository;
import com.skillstorm.finsight.compliance_event.client.SuspectRegistryClient;
import com.skillstorm.finsight.compliance_event.repositories.ComplianceEventSarDetailRepository;
import com.skillstorm.finsight.compliance_event.repositories.SuspectSnapshotAtTimeOfEventRepository;
import com.skillstorm.finsight.compliance_event.models.SuspectSnapshotAtTimeOfEvent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComplianceEventServiceImpl implements ComplianceEventService {

    private final ComplianceEventRepository complianceEventRepository;
    private final ComplianceEventSarDetailRepository sarDetailRepository;
    private final ComplianceEventCtrDetailRepository ctrDetailRepository;
    private final SuspectSnapshotAtTimeOfEventRepository suspectSnapshotRepository;
    private final ComplianceEventMapper mapper;
    private final SuspectRegistryClient suspectRegistryClient;

    public ComplianceEventServiceImpl(
            ComplianceEventRepository complianceEventRepository,
            ComplianceEventSarDetailRepository sarDetailRepository,
            ComplianceEventCtrDetailRepository ctrDetailRepository,
            SuspectSnapshotAtTimeOfEventRepository suspectSnapshotRepository,
            ComplianceEventMapper mapper,
            @Autowired(required = false) SuspectRegistryClient suspectRegistryClient) {

        this.complianceEventRepository = complianceEventRepository;
        this.sarDetailRepository = sarDetailRepository;
        this.ctrDetailRepository = ctrDetailRepository;
        this.suspectSnapshotRepository = suspectSnapshotRepository;
        this.mapper = mapper;
        this.suspectRegistryClient = suspectRegistryClient;
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

        // SAR status must be DRAFT or SUBMITTED (your entity guardrails enforce this)
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
        sarDetailRepository.save(sar);

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
        ctrDetailRepository.save(ctr);

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
    public Page<ComplianceEventResponse> findLinkableByEventType(EventType eventType, Long excludeSuspectId, Pageable pageable) {
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
        complianceEventRepository.save(event);
        return mapper.toResponse(event);
    }

    @Override
    @Transactional
    public ComplianceEventResponse unlinkEventFromSuspect(Long eventId) {
        ComplianceEvent event = complianceEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance event not found: " + eventId));
        event.setSuspectSnapshot(null);
        complianceEventRepository.save(event);
        return mapper.toResponse(event);
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
