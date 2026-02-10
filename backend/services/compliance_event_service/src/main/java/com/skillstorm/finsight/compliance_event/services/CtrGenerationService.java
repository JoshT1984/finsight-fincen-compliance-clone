package com.skillstorm.finsight.compliance_event.services;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillstorm.finsight.compliance_event.mappers.CtrGenerationMapper;
import com.skillstorm.finsight.compliance_event.models.ComplianceEvent;
import com.skillstorm.finsight.compliance_event.repositories.CashTransactionRepository;
import com.skillstorm.finsight.compliance_event.repositories.ComplianceEventCtrDetailRepository;
import com.skillstorm.finsight.compliance_event.repositories.ComplianceEventRepository;
import com.skillstorm.finsight.compliance_event.repositories.CtrAggregationRow;

@Service
public class CtrGenerationService {

    private static final Logger log = LoggerFactory.getLogger(CtrGenerationService.class);

    private final CashTransactionRepository txnRepo;
    private final ComplianceEventRepository eventRepo;
    private final ComplianceEventCtrDetailRepository ctrDetailRepo;

    @NonNull
    private final CtrGenerationMapper mapper;

    public CtrGenerationService(
            CashTransactionRepository txnRepo,
            ComplianceEventRepository eventRepo,
            ComplianceEventCtrDetailRepository ctrDetailRepo,
            @NonNull CtrGenerationMapper mapper) {
        this.txnRepo = txnRepo;
        this.eventRepo = eventRepo;
        this.ctrDetailRepo = ctrDetailRepo;
        this.mapper = mapper;
    }

    @Transactional
    public int generate(LocalDate from, LocalDate to) {
        Instant fromTime = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toTime = to.atStartOfDay(ZoneOffset.UTC).toInstant();

        List<CtrAggregationRow> rows = txnRepo.aggregateCtrCandidates(fromTime, toTime);

        int created = 0;

        for (CtrAggregationRow row : rows) {
            String subjectKey = row.getSubjectKey();
            LocalDate day = row.getTxnDay();
            String idem = "CTR:" + subjectKey + ":" + day;

            if (eventRepo.findByIdempotencyKey(idem).isPresent()) {
                continue;
            }

            Instant dayStart = day.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant dayEnd = day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

            var daySignals = txnRepo.riskSignalsForSubjectDay(subjectKey, dayStart, dayEnd);
            var windowStart = dayStart.minus(java.time.Duration.ofHours(72));
            var windowSignals = txnRepo.riskSignalsForSubjectWindow(subjectKey, windowStart, dayEnd);

            var scoreResult = CtrSuspicionScoring.score(row, daySignals, windowSignals);

            ComplianceEvent event = mapper.toCtrEvent(
                    row.getSubjectKey(),
                    row.getSourceSubjectType(),
                    row.getTxnDay(),
                    row.getTotalCashAmount(),
                    scoreResult.score(),
                    idem);

            ComplianceEvent saved = eventRepo.save(event);

            List<Long> txnIds = txnRepo.findTxnIdsForSubjectDay(subjectKey, dayStart, dayEnd);

            var detail = mapper.toCtrDetail(
                    saved,
                    row,
                    txnIds,
                    scoreResult.score(),
                    scoreResult.band(),
                    scoreResult.drivers());

            // eventType is DB-defaulted and insertable=false, so it may be null in-memory
            // pre-flush.
            log.info("Saving CTR detail for eventId={}, subjectKey={}, day={}, score={}",
                    saved.getEventId(), subjectKey, day, scoreResult.score());

            ctrDetailRepo.save(detail);

            created++;
        }

        return created;
    }

    @Transactional
    public int generateForSubjectDay(String subjectKey, LocalDate day) {
        Instant dayStart = day.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant dayEnd = day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<CtrAggregationRow> rows = txnRepo.aggregateCtrCandidatesForSubjectDay(subjectKey, dayStart, dayEnd);

        int created = 0;

        for (CtrAggregationRow row : rows) {
            String idem = "CTR:" + subjectKey + ":" + day;

            if (eventRepo.findByIdempotencyKey(idem).isPresent()) {
                // CTR already exists for this subject/day, skip (do not throw)
                continue;
            }

            var daySignals = txnRepo.riskSignalsForSubjectDay(subjectKey, dayStart, dayEnd);
            var windowStart = dayStart.minus(java.time.Duration.ofHours(72));
            var windowSignals = txnRepo.riskSignalsForSubjectWindow(subjectKey, windowStart, dayEnd);

            var scoreResult = CtrSuspicionScoring.score(row, daySignals, windowSignals);

            ComplianceEvent event = mapper.toCtrEvent(
                    row.getSubjectKey(),
                    row.getSourceSubjectType(),
                    row.getTxnDay(),
                    row.getTotalCashAmount(),
                    scoreResult.score(),
                    idem);

            ComplianceEvent saved = eventRepo.save(event);

            List<Long> txnIds = txnRepo.findTxnIdsForSubjectDay(subjectKey, dayStart, dayEnd);

            var detail = mapper.toCtrDetail(
                    saved,
                    row,
                    txnIds,
                    scoreResult.score(),
                    scoreResult.band(),
                    scoreResult.drivers());

            log.info("Saving CTR detail for eventId={}, subjectKey={}, day={}, score={}",
                    saved.getEventId(), subjectKey, day, scoreResult.score());

            ctrDetailRepo.save(detail);

            created++;
        }

        return created;
    }
}
