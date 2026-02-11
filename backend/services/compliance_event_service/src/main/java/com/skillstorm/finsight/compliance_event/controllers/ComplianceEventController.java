package com.skillstorm.finsight.compliance_event.controllers;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.skillstorm.finsight.compliance_event.dtos.ComplianceEventResponse;
import com.skillstorm.finsight.compliance_event.dtos.CreateCtrRequest;
import com.skillstorm.finsight.compliance_event.dtos.CreateSarRequest;
import com.skillstorm.finsight.compliance_event.dtos.CtrDetailResponse;
import com.skillstorm.finsight.compliance_event.dtos.LinkEventToSuspectRequest;
import com.skillstorm.finsight.compliance_event.models.EventStatus;
import com.skillstorm.finsight.compliance_event.models.EventType;
import com.skillstorm.finsight.compliance_event.services.ComplianceEventService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/compliance-events")
@Validated
public class ComplianceEventController {

    private final ComplianceEventService service;

    public ComplianceEventController(ComplianceEventService service) {
        this.service = service;
    }

    // ---------- CREATE ----------

    @PostMapping("/sar")
    @ResponseStatus(HttpStatus.CREATED)
    public ComplianceEventResponse createSar(@Valid @RequestBody CreateSarRequest request) {
        return service.createSar(request);
    }

    @PostMapping("/ctr")
    @ResponseStatus(HttpStatus.CREATED)
    public ComplianceEventResponse createCtr(@Valid @RequestBody CreateCtrRequest request) {
        return service.createCtr(request);
    }

    // ---------- READ ----------

    @GetMapping("/{eventId}")
    public ComplianceEventResponse getById(@PathVariable Long eventId) {
        return service.getById(eventId);
    }

    @GetMapping("/{eventId}/ctr-detail")
    public CtrDetailResponse getCtrDetail(@PathVariable Long eventId) {
        return service.getCtrDetail(eventId);
    }

    @PostMapping("/{ctrEventId}/generate-sar")
    @ResponseStatus(HttpStatus.CREATED)
    public ComplianceEventResponse generateSarFromCtr(@PathVariable Long ctrEventId) {
        return service.generateSarFromCtr(ctrEventId);
    }

    // ---------- SEARCH ----------

    @GetMapping
    public Page<ComplianceEventResponse> search(
            @RequestParam(required = false) EventType eventType,
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) String sourceSystem,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Long suspectId,
            @RequestParam(required = false) Long notLinkedToSuspectId,
            Pageable pageable) {

        if (suspectId != null) {
            return service.findBySuspectId(suspectId, pageable);
        }

        if (notLinkedToSuspectId != null && eventType != null) {
            return service.findLinkableByEventType(eventType, notLinkedToSuspectId, pageable);
        }

        return service.search(eventType, status, sourceSystem, from, to, pageable);
    }

    // ---------- SUSPECT LINKING ----------

    @PutMapping("/{eventId}/suspect")
    public ComplianceEventResponse linkEventToSuspect(
            @PathVariable Long eventId,
            @Valid @RequestBody LinkEventToSuspectRequest request) {
        return service.linkEventToSuspect(eventId, request.suspectId());
    }

    @DeleteMapping("/{eventId}/suspect")
    public ComplianceEventResponse unlinkEventFromSuspect(@PathVariable Long eventId) {
        return service.unlinkEventFromSuspect(eventId);
    }
}
