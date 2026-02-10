package com.skillstorm.finsight.compliance_event.services;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillstorm.finsight.compliance_event.dtos.ComplianceEventResponse;
import com.skillstorm.finsight.compliance_event.dtos.CreateCtrRequest;
import com.skillstorm.finsight.compliance_event.dtos.CreateSarRequest;
import com.skillstorm.finsight.compliance_event.dtos.CtrDetailResponse;
import com.skillstorm.finsight.compliance_event.exceptions.ResourceConflictException;
import com.skillstorm.finsight.compliance_event.exceptions.ResourceNotFoundException;
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

    public ComplianceEventServiceImpl(
            ComplianceEventRepository complianceEventRepository,
            ComplianceEventSarDetailRepository sarDetailRepository,
            ComplianceEventCtrDetailRepository ctrDetailRepository,
            SuspectSnapshotAtTimeOfEventRepository suspectSnapshotRepository,
            ComplianceEventMapper mapper) {

        this.complianceEventRepository = complianceEventRepository;
        this.sarDetailRepository = sarDetailRepository;
        this.ctrDetailRepository = ctrDetailRepository;
        this.suspectSnapshotRepository = suspectSnapshotRepository;
        this.mapper = mapper;
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
        sarDetailRepository.save(sar);

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
        ctrDetailRepository.save(ctr);

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

        Map<String, Object> ctrFormData =
                detail.getCtrFormData() == null ? Map.of() : detail.getCtrFormData();

        List<Long> contributingTxnIds = extractTxnIds(ctrFormData);

        List<ComplianceEventResponse> priorCtrs =
                complianceEventRepository
                        .findByExternalSubjectKeyAndEventTypeOrderByEventTimeDesc(
                                event.getExternalSubjectKey(),
                                EventType.CTR
                        )
                        .stream()
                        .filter(e -> !e.getEventId().equals(eventId)) // change to getEventId() if needed
                        .map(mapper::toResponse)
                        .toList();

        return new CtrDetailResponse(
                mapper.toResponse(event),
                ctrFormData,
                contributingTxnIds,
                priorCtrs
        );
    }

    @SuppressWarnings("unchecked")
    private List<Long> extractTxnIds(Map<String, Object> ctrFormData) {
        Object raw = ctrFormData.get("contributingTxnIds");
        if (raw == null) return List.of();

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
