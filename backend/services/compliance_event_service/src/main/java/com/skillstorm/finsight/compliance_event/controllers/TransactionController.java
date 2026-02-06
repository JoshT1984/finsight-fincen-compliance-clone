package com.skillstorm.finsight.compliance_event.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.skillstorm.finsight.compliance_event.dtos.CreateTransactionRequest;
import com.skillstorm.finsight.compliance_event.dtos.TransactionResponse;
import com.skillstorm.finsight.compliance_event.mappers.CashTransactionMapper;
import com.skillstorm.finsight.compliance_event.models.CashTransaction;
import com.skillstorm.finsight.compliance_event.repositories.CashTransactionRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

  private final CashTransactionRepository repo;
  private final CashTransactionMapper mapper;

  public TransactionController(CashTransactionRepository repo, CashTransactionMapper mapper) {
    this.repo = repo;
    this.mapper = mapper;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public TransactionResponse create(@Valid @RequestBody CreateTransactionRequest request) {
    CashTransaction entity = mapper.toEntity(request);
    CashTransaction saved = repo.save(entity);
    return mapper.toResponse(saved);
  }
}
