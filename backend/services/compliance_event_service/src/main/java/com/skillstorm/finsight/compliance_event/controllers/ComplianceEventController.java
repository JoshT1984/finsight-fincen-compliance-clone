package com.skillstorm.finsight.compliance_event.controllers;

import java.time.Instant;

import com.skillstorm.finsight.compliance_event.dtos.ComplianceEventResponse;
import com.skillstorm.finsight.compliance_event.dtos.CreateCtrRequest;
import com.skillstorm.finsight.compliance_event.dtos.CreateSarRequest;
import com.skillstorm.finsight.compliance_event.models.EventStatus;
import com.skillstorm.finsight.compliance_event.models.EventType;
import com.skillstorm.finsight.compliance_event.services.ComplianceEventService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/compliance-events")
@Validated
public class ComplianceEventController {

    private final ComplianceEventService service;

    public ComplianceEventController(ComplianceEventService service) {
        this.service = service;
    }

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

    @GetMapping("/{eventId}")
    public ComplianceEventResponse getById(@PathVariable Long eventId) {
        return service.getById(eventId);
    }

    @GetMapping
    public Page<ComplianceEventResponse> search(
            @RequestParam(required = false) EventType eventType,
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) String sourceSystem,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) Instant to,
            Pageable pageable) {

        return service.search(eventType, status, sourceSystem, from, to, pageable);
    }
}
