package com.skillstorm.finsight.compliance_event.controllers;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.StringJoiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.server.ResponseStatusException;

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

        private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

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

                var pageable = PageRequest.of(
                                page,
                                Math.min(size, 500),
                                Sort.by(Sort.Direction.DESC, "txnTime"));

                return repo.findAll(pageable).map(mapper::toResponse);
        }

        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        public TransactionResponse create(@Valid @RequestBody CreateTransactionRequest request) {

                // Map request -> entity
                CashTransaction entity = mapper.toEntity(request);

                // Hard guard: if externalSubjectKey is missing, you MUST provide type + id.
                // Otherwise Java can generate "null" keys that will never match SQL
                // aggregation.
                boolean hasExternalKey = entity.getExternalSubjectKey() != null
                                && !entity.getExternalSubjectKey().isBlank();
                if (!hasExternalKey) {
                        if (entity.getSourceSubjectType() == null || entity.getSourceSubjectType().isBlank()
                                        || entity.getSourceSubjectId() == null
                                        || entity.getSourceSubjectId().isBlank()) {
                                throw new ResponseStatusException(
                                                HttpStatus.BAD_REQUEST,
                                                "Either externalSubjectKey must be provided, OR sourceSubjectType and sourceSubjectId must both be provided.");
                        }
                }

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

                // Derive subjectKey consistently with SQL SUBJECT_KEY_EXPR logic:
                // COALESCE(NULLIF(external_subject_key,''), CONCAT_WS(':', source_system,
                // source_subject_type, source_subject_id))
                String subjectKey = deriveSubjectKey(saved);

                // Derive UTC day window
                LocalDate day = saved.getTxnTime()
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate();

                log.info("CTR trigger -> subjectKey={} day(UTC)={}", subjectKey, day);

                // Emit event for real-time processing
                String trigger = SecurityContextHolder.getContext().getAuthentication() != null
                                ? "USER"
                                : "SYSTEM";

                try {
                        int created = ctrGenerationService.generateForSubjectDay(subjectKey, day);

                        log.info("CTR result -> subjectKey={} day(UTC)={} created={}",
                                        subjectKey, day, created);

                } catch (Exception e) {
                        log.error("CTR generation FAILED -> subjectKey={} day(UTC)={}",
                                        subjectKey, day, e);
                }

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

                return mapper.toResponse(saved);
        }

        private String deriveSubjectKey(CashTransaction t) {
                if (t.getExternalSubjectKey() != null && !t.getExternalSubjectKey().isBlank()) {
                        return t.getExternalSubjectKey().trim();
                }

                // Mimic CONCAT_WS(':', a, b, c) behavior by joining non-null parts with ':'
                // (but controller guard ensures type + id are present when ext key is absent).
                StringJoiner joiner = new StringJoiner(":");
                if (t.getSourceSystem() != null && !t.getSourceSystem().isBlank()) {
                        joiner.add(t.getSourceSystem().trim());
                }
                if (t.getSourceSubjectType() != null && !t.getSourceSubjectType().isBlank()) {
                        joiner.add(t.getSourceSubjectType().trim());
                }
                if (t.getSourceSubjectId() != null && !t.getSourceSubjectId().isBlank()) {
                        joiner.add(t.getSourceSubjectId().trim());
                }
                return joiner.toString();
        }
}
