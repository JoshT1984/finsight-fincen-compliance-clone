package com.skillstorm.finsight.compliance_event.services;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillstorm.finsight.compliance_event.emitters.ComplianceEventEmitter;
import com.skillstorm.finsight.compliance_event.loggers.ComplianceEventLog;
import com.skillstorm.finsight.compliance_event.mappers.CtrGenerationMapper;
import com.skillstorm.finsight.compliance_event.models.ComplianceEvent;
import com.skillstorm.finsight.compliance_event.models.EventType;
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
    private final SarPromotionService sarPromotionService;

    private final ComplianceEventEmitter complianceEmitter;

    @NonNull
    private final CtrGenerationMapper mapper;

    public CtrGenerationService(
            CashTransactionRepository txnRepo,
            ComplianceEventRepository eventRepo,
            ComplianceEventCtrDetailRepository ctrDetailRepo,
            SarPromotionService sarPromotionService,
            ComplianceEventEmitter complianceEmitter,
            @NonNull CtrGenerationMapper mapper) {

        this.txnRepo = txnRepo;
        this.eventRepo = eventRepo;
        this.ctrDetailRepo = ctrDetailRepo;
        this.sarPromotionService = sarPromotionService;
        this.mapper = mapper;
        this.complianceEmitter = complianceEmitter;
    }

    @Transactional
    public int generate(LocalDate from, LocalDate to) {
        Instant fromTime = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toTime = to.atStartOfDay(ZoneOffset.UTC).toInstant();

        List<CtrAggregationRow> rows = txnRepo.aggregateCtrCandidates(fromTime, toTime);
        log.info("CTR generate range -> from={} to={} candidates={}", from, to, rows.size());

        int created = 0;

        for (CtrAggregationRow row : rows) {
            String subjectKey = row.getSubjectKey();
            LocalDate day = row.getTxnDay();

            created += generateSingleRow(subjectKey, day, row);
        }

        return created;
    }

    @Transactional
    public int generateForSubjectDay(String subjectKey, LocalDate day) {
        Instant dayStart = day.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant dayEnd = day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<CtrAggregationRow> rows = txnRepo.aggregateCtrCandidatesForSubjectDay(subjectKey, dayStart,
                dayEnd);

        log.info("CTR generateForSubjectDay -> subjectKey={} day={} rows={}", subjectKey, day, rows.size());

        int created = 0;

        for (CtrAggregationRow row : rows) {
            created += generateSingleRow(subjectKey, day, row);
        }

        return created;
    }

    /**
     * Handles both range generation and single-day generation consistently.
     */
    private int generateSingleRow(String subjectKey, LocalDate day, CtrAggregationRow row) {
        Instant dayStart = day.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant dayEnd = day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        // Old idempotency key (kept for backward compatibility + log correlation)
        String idem = "CTR:" + subjectKey + ":" + day;

        // ✅ Deterministic idempotency check:
        // First check “does a CTR already exist for this subject+UTC day”
        Optional<ComplianceEvent> existingByDay = eventRepo.findCtrForSubjectDay(EventType.CTR, subjectKey,
                dayStart, dayEnd);
        if (existingByDay.isPresent()) {
            ComplianceEvent existingCtr = existingByDay.get();

            log.info("CTR already exists (by subject+day) -> eventId={} subjectKey={} day={}",
                    existingCtr.getEventId(), subjectKey, day);

            reEvaluatePromotion(existingCtr, subjectKey, day, row, dayStart, dayEnd);
            return 0;
        }

        // ✅ FIX: Only treat this idempotency key as “existing” if it is a CTR.
        var existingByIdem = eventRepo.findByIdempotencyKeyAndEventType(idem, EventType.CTR);
        if (existingByIdem.isPresent()) {
            ComplianceEvent existingCtr = existingByIdem.get();

            log.info("CTR already exists (by idempotencyKey+eventType) -> eventId={} idem={} subjectKey={} day={}",
                    existingCtr.getEventId(), idem, subjectKey, day);

            reEvaluatePromotion(existingCtr, subjectKey, day, row, dayStart, dayEnd);
            return 0;
        }

        // Compute signals and score
        var daySignals = txnRepo.riskSignalsForSubjectDay(subjectKey, dayStart, dayEnd);
        var windowStart = dayStart.minus(java.time.Duration.ofHours(72));
        var windowSignals = txnRepo.riskSignalsForSubjectWindow(subjectKey, windowStart, dayEnd);

        var scoreResult = scoreWithExternalSignals(subjectKey, row, dayStart, dayEnd, daySignals,
                windowSignals);

        log.info("Creating CTR -> subjectKey={} day={} totalCashAmount={} score={}",
                subjectKey, day, row.getTotalCashAmount(), scoreResult.score());

        ComplianceEvent event = mapper.toCtrEvent(
                row.getSubjectKey(),
                row.getSourceSubjectType(),
                row.getTxnDay(),
                row.getTotalCashAmount(),
                scoreResult.score(),
                idem);

        ComplianceEvent saved = eventRepo.save(event);

        // Emit for logging purposes
        complianceEmitter.emit(
                ComplianceEventLog.ctrCreated(saved.getEventId().toString(), idem,
                        "CTR_GENERATION",
                        Map.of(
                                "subjectKey", row.getSubjectKey(),
                                "day", row.getTxnDay().toString(),
                                "totalCashAmount", row.getTotalCashAmount(),
                                "score", scoreResult.score())));

        // Attach txn IDs and save detail
        List<Long> txnIds = txnRepo.findTxnIdsForSubjectDay(subjectKey, dayStart, dayEnd);

        var detail = mapper.toCtrDetail(
                saved,
                row,
                txnIds,
                scoreResult.score(),
                scoreResult.band(),
                scoreResult.drivers());

        log.info("Saving CTR detail -> eventId={} subjectKey={} day={} score={}",
                saved.getEventId(), subjectKey, day, scoreResult.score());

        ctrDetailRepo.save(detail);

        // ✅ Promotion should not be allowed to kill CTR creation.
        try {
            sarPromotionService.promoteFromCtr(saved, scoreResult);
        } catch (Exception e) {
            log.warn("CTR created but SAR promotion (and/or case creation) failed -> ctrEventId={} reason={}",
                    saved.getEventId(), e.toString());
        }

        return 1;
    }

    private void reEvaluatePromotion(
            ComplianceEvent existingCtr,
            String subjectKey,
            LocalDate day,
            CtrAggregationRow row,
            Instant dayStart,
            Instant dayEnd) {

        var daySignals = txnRepo.riskSignalsForSubjectDay(subjectKey, dayStart, dayEnd);
        var windowStart = dayStart.minus(java.time.Duration.ofHours(72));
        var windowSignals = txnRepo.riskSignalsForSubjectWindow(subjectKey, windowStart, dayEnd);

        var scoreResult = scoreWithExternalSignals(subjectKey, row, dayStart, dayEnd, daySignals,
                windowSignals);

        log.info("Re-evaluating SAR promotion from existing CTR -> eventId={} subjectKey={} day={} score={}",
                existingCtr.getEventId(), subjectKey, day, scoreResult.score());

        try {
            sarPromotionService.promoteFromCtr(existingCtr, scoreResult);
        } catch (Exception e) {
            log.warn("Re-evaluation promotion failed -> ctrEventId={} reason={}",
                    existingCtr.getEventId(), e.toString());
        }
    }

    private CtrSuspicionScoring.ScoreResult scoreWithExternalSignals(
            String subjectKey,
            CtrAggregationRow row,
            Instant dayStart,
            Instant dayEnd,
            com.skillstorm.finsight.compliance_event.repositories.CtrRiskSignalsRow daySignals,
            com.skillstorm.finsight.compliance_event.repositories.CtrRiskSignalsRow windowSignals) {

        // Velocity baseline: previous 30 days average daily cash total
        Instant baselineStart = dayStart.minus(java.time.Duration.ofDays(30));
        java.math.BigDecimal baselineAvg = null;

        try {
            baselineAvg = txnRepo.avgDailyCashTotalForSubject(subjectKey, baselineStart, dayStart);
        } catch (Exception ignored) {
            // Non-fatal: scoring falls back
        }

        boolean highRiskGeo = false;
        boolean addressMismatch = false;

        try {
            var locations = txnRepo.distinctLocationsForSubjectDay(subjectKey, dayStart, dayEnd);

            // High-risk geography keyword match
            highRiskGeo = HighRiskGeography.isHighRisk(locations);

            // Address anomaly from dropdown keywords
            addressMismatch = AddressAnomaly.isUnknownOrMissing(locations);

        } catch (Exception ignored) {
            // Non-fatal
        }

        var external = new CtrSuspicionScoring.ExternalSignals(
                addressMismatch, // +25 if true
                false, // linkedAccounts (future wiring)
                highRiskGeo, // +25 if true
                baselineAvg // velocity spike calculation
        );

        return CtrSuspicionScoring.score(row, daySignals, windowSignals, external);
    }
}