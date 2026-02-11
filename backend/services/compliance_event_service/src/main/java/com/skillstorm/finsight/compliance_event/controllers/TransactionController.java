package com.skillstorm.finsight.compliance_event.controllers;

import java.time.LocalDate;
import java.time.ZoneOffset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.skillstorm.finsight.compliance_event.dtos.CreateTransactionRequest;
import com.skillstorm.finsight.compliance_event.dtos.TransactionResponse;
import com.skillstorm.finsight.compliance_event.mappers.CashTransactionMapper;
import com.skillstorm.finsight.compliance_event.models.CashTransaction;
import com.skillstorm.finsight.compliance_event.repositories.CashTransactionRepository;
import com.skillstorm.finsight.compliance_event.services.CtrGenerationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

  private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

  private final CashTransactionRepository repo;
  private final CashTransactionMapper mapper;
  private final CtrGenerationService ctrGenerationService;

  public TransactionController(
      CashTransactionRepository repo,
      CashTransactionMapper mapper,
      CtrGenerationService ctrGenerationService) {
    this.repo = repo;
    this.mapper = mapper;
    this.ctrGenerationService = ctrGenerationService;
  }

  @GetMapping
  public Page<TransactionResponse> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {

    var pageable = PageRequest.of(
        page,
        Math.min(size, 500),
        Sort.by(Sort.Direction.DESC, "txnTime"));

    return repo.findAll(pageable).map(mapper::toResponse);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public TransactionResponse create(@Valid @RequestBody CreateTransactionRequest request) {

    CashTransaction entity = mapper.toEntity(request);
    CashTransaction saved = repo.save(entity);

    log.info("Transaction saved -> txnId={} extKey={} source={}:{}:{} time={} cashIn={} cashOut={}",
        saved.getTxnId(),
        saved.getExternalSubjectKey(),
        saved.getSourceSystem(),
        saved.getSourceSubjectType(),
        saved.getSourceSubjectId(),
        saved.getTxnTime(),
        saved.getCashIn(),
        saved.getCashOut());

    // Derive subjectKey
    String subjectKey = (saved.getExternalSubjectKey() != null && !saved.getExternalSubjectKey().isBlank())
        ? saved.getExternalSubjectKey()
        : String.format("%s:%s:%s",
            saved.getSourceSystem(),
            saved.getSourceSubjectType(),
            saved.getSourceSubjectId());

    // Derive UTC day window
    LocalDate day = saved.getTxnTime()
        .atZone(ZoneOffset.UTC)
        .toLocalDate();

    log.info("CTR trigger -> subjectKey={} day(UTC)={}", subjectKey, day);

    try {
      int created = ctrGenerationService.generateForSubjectDay(subjectKey, day);

      log.info("CTR result -> subjectKey={} day(UTC)={} created={}",
          subjectKey, day, created);

    } catch (Exception e) {
      log.error("CTR generation FAILED -> subjectKey={} day(UTC)={}",
          subjectKey, day, e);
    }

    return mapper.toResponse(saved);
  }
}
