package com.skillstorm.finsight.compliance_event.controllers;

import java.time.LocalDate;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skillstorm.finsight.compliance_event.services.CtrGenerationService;

@RestController
@RequestMapping("/api/ctrs")
public class CtrGenerationController {

    private final CtrGenerationService service;

    public CtrGenerationController(CtrGenerationService service) {
        this.service = service;
    }

    @PostMapping("/generate")
    public int generate(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to
    ) {
        return service.generate(from, to);
    }
}
