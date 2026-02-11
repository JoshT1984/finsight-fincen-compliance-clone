package com.skillstorm.finsight.compliance_event.controllers;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.skillstorm.finsight.compliance_event.dtos.CreateTransactionRequest;
import com.skillstorm.finsight.compliance_event.dtos.TransactionResponse;
import com.skillstorm.finsight.compliance_event.emitters.ComplianceEventEmitter;
import com.skillstorm.finsight.compliance_event.loggers.ComplianceEventLog;
import com.skillstorm.finsight.compliance_event.mappers.CashTransactionMapper;
import com.skillstorm.finsight.compliance_event.models.CashTransaction;
import com.skillstorm.finsight.compliance_event.repositories.CashTransactionRepository;
import com.skillstorm.finsight.compliance_event.services.CtrGenerationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

  private final CashTransactionRepository repo;
  private final CashTransactionMapper mapper;
  private final CtrGenerationService ctrGenerationService;
  private final ComplianceEventEmitter complianceEventEmitter;

  public TransactionController(CashTransactionRepository repo, CashTransactionMapper mapper,
      CtrGenerationService ctrGenerationService, ComplianceEventEmitter complianceEventEmitter) {
    this.repo = repo;
    this.mapper = mapper;
    this.ctrGenerationService = ctrGenerationService;
    this.complianceEventEmitter = complianceEventEmitter;
  }

  @GetMapping
  public Page<TransactionResponse> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    // Keep this lightweight: newest first
    var pageable = PageRequest.of(page, Math.min(size, 500), Sort.by(Sort.Direction.DESC, "txnTime"));
    return repo.findAll(pageable).map(mapper::toResponse);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public TransactionResponse create(@Valid @RequestBody CreateTransactionRequest request) {
    CashTransaction entity = mapper.toEntity(request);
    CashTransaction saved = repo.save(entity);

    // Derive subjectKey and day from saved transaction
    java.time.LocalDate day = saved.getTxnTime().atZone(java.time.ZoneOffset.UTC).toLocalDate();
    String subjectKey = (saved.getExternalSubjectKey() != null && !saved.getExternalSubjectKey().isBlank())
        ? saved.getExternalSubjectKey()
        : String.format("%s:%s:%s", saved.getSourceSystem(), saved.getSourceSubjectType(), saved.getSourceSubjectId());

    // Emit event for real-time processing
    String trigger = SecurityContextHolder.getContext().getAuthentication() != null
        ? "USER"
        : "SYSTEM";

    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("cashIn", saved.getCashIn());
    metadata.put("cashOut", saved.getCashOut());
    metadata.put("netCash", saved.getCashIn().subtract(saved.getCashOut()));
    metadata.put("txnTime", saved.getTxnTime());
    metadata.put("subjectKey", subjectKey);
    metadata.put("sourceSystem", saved.getSourceSystem());

    ComplianceEventLog log = new ComplianceEventLog(
        Instant.now(),
        "TRANSACTION",
        saved.getTxnId().toString(),
        "CREATED",
        trigger,
        "MANUAL_TRANSACTION_CREATE",
        "TXN:" + saved.getTxnId(),
        metadata);

    complianceEventEmitter.emit(log);

    try {
      int created = ctrGenerationService.generateForSubjectDay(subjectKey, day);
      org.slf4j.LoggerFactory.getLogger(TransactionController.class)
          .info("CTR generation attempt subjectKey={}, day={}, created={}", subjectKey, day, created);
    } catch (Exception e) {
      org.slf4j.LoggerFactory.getLogger(TransactionController.class)
          .warn("CTR generation failed (non-fatal) subjectKey={}, day={} : {}", subjectKey, day, e.getMessage(), e);
    }

    return mapper.toResponse(saved);
  }
}
