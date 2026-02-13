package com.skillstorm.finsight.compliance_event.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillstorm.finsight.compliance_event.mappers.CtrGenerationMapper;
import com.skillstorm.finsight.compliance_event.models.ComplianceEvent;
import com.skillstorm.finsight.compliance_event.models.ComplianceEventCtrDetail;
import com.skillstorm.finsight.compliance_event.repositories.CashTransactionRepository;
import com.skillstorm.finsight.compliance_event.repositories.ComplianceEventCtrDetailRepository;
import com.skillstorm.finsight.compliance_event.repositories.ComplianceEventRepository;
import com.skillstorm.finsight.compliance_event.repositories.CtrAggregationRow;
import com.skillstorm.finsight.compliance_event.repositories.CtrRiskSignalsRow;

@Service
public class CtrGenerationService {

    private static final Logger log = LoggerFactory.getLogger(CtrGenerationService.class);

    private final CashTransactionRepository cashTxRepo;
    private final ComplianceEventRepository eventRepo;
    private final ComplianceEventCtrDetailRepository ctrDetailRepo;
    private final CtrGenerationMapper mapper;
    private final SarPromotionService sarPromotionService;

    public CtrGenerationService(
            CashTransactionRepository cashTxRepo,
            ComplianceEventRepository eventRepo,
            ComplianceEventCtrDetailRepository ctrDetailRepo,
            CtrGenerationMapper mapper,
            SarPromotionService sarPromotionService) {
        this.cashTxRepo = cashTxRepo;
        this.eventRepo = eventRepo;
        this.ctrDetailRepo = ctrDetailRepo;
        this.mapper = mapper;
        this.sarPromotionService = sarPromotionService;
    }

    @Transactional
    public int generateForSubjectDay(String subjectKey, LocalDate dayUtc) {
        Instant fromTime = dayUtc.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toTime = dayUtc.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<CtrAggregationRow> rows = cashTxRepo.aggregateCtrCandidatesForSubjectDay(subjectKey, fromTime, toTime);

        log.info("CTR generateForSubjectDay -> subjectKey={} dayUtc={} fromTime={} toTime={} rows={}",
                subjectKey, dayUtc, fromTime, toTime, rows.size());

        int count = 0;
        for (CtrAggregationRow row : rows) {
            if (row.getTotalCashAmount() == null)
                continue;
            if (row.getTotalCashAmount().signum() <= 0)
                continue;

            upsertCtrFromAggregation(row);
            count++;
        }
        return count;
    }

    private void upsertCtrFromAggregation(CtrAggregationRow row) {
        String idempotencyKey = mapper.ctrIdempotencyKey(row.getSubjectKey(), row.getTxnDay());

        Optional<ComplianceEvent> existingOpt = eventRepo.findByIdempotencyKey(idempotencyKey);

        ComplianceEvent saved;
        if (existingOpt.isPresent()) {
            ComplianceEvent existing = existingOpt.get();
            ComplianceEvent fresh = mapper.toCtrEvent(row);

            existing.setTotalAmount(fresh.getTotalAmount());
            existing.setSeverityScore(fresh.getSeverityScore());
            existing.setEventTime(fresh.getEventTime());

            existing.setExternalSubjectKey(fresh.getExternalSubjectKey());
            existing.setSourceSubjectType(fresh.getSourceSubjectType());
            existing.setSourceSubjectId(fresh.getSourceSubjectId());

            saved = eventRepo.save(existing);

            log.info("Updating CTR -> eventId={} subjectKey={} txnDay={} totalCashAmount={} score={}",
                    saved.getEventId(), row.getSubjectKey(), row.getTxnDay(),
                    saved.getTotalAmount(), saved.getSeverityScore());
        } else {
            ComplianceEvent ev = mapper.toCtrEvent(row);

            try {
                saved = eventRepo.save(ev);
                log.info("Creating CTR -> subjectKey={} txnDay={} totalCashAmount={} score={} eventId={}",
                        row.getSubjectKey(), row.getTxnDay(),
                        saved.getTotalAmount(), saved.getSeverityScore(), saved.getEventId());
            } catch (DataIntegrityViolationException dive) {
                log.warn("CTR insert collided (idempotencyKey={}). Refetching.", ev.getIdempotencyKey());
                saved = eventRepo.findByIdempotencyKey(ev.getIdempotencyKey())
                        .orElseThrow(() -> dive);
            }
        }

        upsertCtrDetail(saved, row);

        // -----------------------------
        // CTR -> SAR Promotion (real scoring)
        // -----------------------------
        Instant dayStart = row.getTxnDay().atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant dayEnd = row.getTxnDay().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        // Day signals (same day window as aggregation)
        CtrRiskSignalsRow daySignals = cashTxRepo.riskSignalsForSubjectDay(row.getSubjectKey(), dayStart, dayEnd);

        // 72-hour window signals (supporting high-frequency signal)
        Instant windowStart = dayStart.minus(72, ChronoUnit.HOURS);
        CtrRiskSignalsRow windowSignals = cashTxRepo.riskSignalsForSubjectWindow(row.getSubjectKey(), windowStart,
                dayEnd);

        // Baseline average daily total (e.g., prior 30 days), fed into ExternalSignals
        Instant baselineStart = dayStart.minus(30, ChronoUnit.DAYS);
        BigDecimal baselineAvg = cashTxRepo.avgDailyCashTotalForSubject(row.getSubjectKey(), baselineStart, dayStart);

        CtrSuspicionScoring.ExternalSignals external = new CtrSuspicionScoring.ExternalSignals(false, false, false,
                baselineAvg);

        CtrSuspicionScoring.ScoreResult score = CtrSuspicionScoring.score(row, daySignals, windowSignals, external);

        // keep CTR severityScore aligned with scoring (optional but nice)
        if (saved.getSeverityScore() == null || !saved.getSeverityScore().equals(score.score())) {
            saved.setSeverityScore(score.score());
            saved = eventRepo.save(saved);
        }

        sarPromotionService.promoteFromCtr(saved, score);
    }

    private void upsertCtrDetail(ComplianceEvent savedEvent, CtrAggregationRow row) {
        Long eventId = savedEvent.getEventId();

        Optional<ComplianceEventCtrDetail> detailOpt = ctrDetailRepo.findById(eventId);
        if (detailOpt.isPresent()) {
            ComplianceEventCtrDetail existing = detailOpt.get();
            ComplianceEventCtrDetail fresh = mapper.toCtrDetail(savedEvent, row);

            existing.setCustomerName(fresh.getCustomerName());
            existing.setTransactionTime(fresh.getTransactionTime());
            existing.setCtrFormData(fresh.getCtrFormData());

            ctrDetailRepo.save(existing);

            log.info("Updating CTR detail -> eventId={} subjectKey={} txnDay={} score={}",
                    eventId, row.getSubjectKey(), row.getTxnDay(), savedEvent.getSeverityScore());
            return;
        }

        ComplianceEventCtrDetail newDetail = mapper.toCtrDetail(savedEvent, row);

        try {
            ctrDetailRepo.save(newDetail);
            log.info("Saving CTR detail -> eventId={} subjectKey={} txnDay={} score={}",
                    eventId, row.getSubjectKey(), row.getTxnDay(), savedEvent.getSeverityScore());
        } catch (DataIntegrityViolationException dive) {
            log.warn("CTR detail insert collided (eventId={}). Refetching and updating.", eventId);

            ComplianceEventCtrDetail existing = ctrDetailRepo.findById(eventId)
                    .orElseThrow(() -> dive);

            ComplianceEventCtrDetail fresh = mapper.toCtrDetail(savedEvent, row);
            existing.setCustomerName(fresh.getCustomerName());
            existing.setTransactionTime(fresh.getTransactionTime());
            existing.setCtrFormData(fresh.getCtrFormData());

            ctrDetailRepo.save(existing);
        }
    }
}
