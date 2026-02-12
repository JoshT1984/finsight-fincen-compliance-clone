package com.skillstorm.finsight.compliance_event.controllers;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.skillstorm.finsight.compliance_event.services.CtrGenerationService;

@RestController
@RequestMapping("/api/debug/ctr")
public class CtrDebugController {

    private static final Logger log = LoggerFactory.getLogger(CtrDebugController.class);

    private final CtrGenerationService ctrGenerationService;

    public CtrDebugController(CtrGenerationService ctrGenerationService) {
        this.ctrGenerationService = ctrGenerationService;
    }

    /**
     * Example:
     * GET /api/debug/ctr/generate?subjectKey=CUST-102994&day=2026-02-11
     */
    @GetMapping("/generate")
    @ResponseStatus(HttpStatus.OK)
    public String generate(
            @RequestParam String subjectKey,
            @RequestParam String day) {

        LocalDate utcDay = LocalDate.parse(day);

        log.info("DEBUG CTR generate request -> subjectKey={} day={}", subjectKey, utcDay);

        int created = ctrGenerationService.generateForSubjectDay(subjectKey, utcDay);

        log.info("DEBUG CTR generate result -> subjectKey={} day={} created={}", subjectKey, utcDay, created);

        return "created=" + created;
    }
}