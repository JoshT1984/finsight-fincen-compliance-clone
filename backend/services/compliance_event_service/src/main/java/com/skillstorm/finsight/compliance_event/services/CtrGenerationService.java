package com.skillstorm.finsight.compliance_event.services;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillstorm.finsight.compliance_event.emitters.ComplianceEventEmitter;
import com.skillstorm.finsight.compliance_event.loggers.ComplianceEventLog;
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

                int created = 0;

                for (CtrAggregationRow row : rows) {
                        String subjectKey = row.getSubjectKey();
                        LocalDate day = row.getTxnDay();
                        String idem = "CTR:" + subjectKey + ":" + day;

                        var existingOpt = eventRepo.findByIdempotencyKey(idem);
                        if (existingOpt.isPresent()) {
                                // CTR immutable, but promotion can still be evaluated using latest day signals
                                ComplianceEvent existingCtr = existingOpt.get();

                                Instant dayStart = day.atStartOfDay(ZoneOffset.UTC).toInstant();
                                Instant dayEnd = day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

                                var daySignals = txnRepo.riskSignalsForSubjectDay(subjectKey, dayStart, dayEnd);
                                var windowStart = dayStart.minus(java.time.Duration.ofHours(72));
                                var windowSignals = txnRepo.riskSignalsForSubjectWindow(subjectKey, windowStart,
                                                dayEnd);

                                var scoreResult = scoreWithExternalSignals(subjectKey, row, dayStart, dayEnd,
                                                daySignals,
                                                windowSignals);

                                log.info("CTR already exists for subjectKey={}, day={}. Re-evaluating SAR promotion with score={}",
                                                subjectKey, day, scoreResult.score());

                                sarPromotionService.promoteFromCtr(existingCtr, scoreResult);
                                continue;
                        }

                        Instant dayStart = day.atStartOfDay(ZoneOffset.UTC).toInstant();
                        Instant dayEnd = day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

                        var daySignals = txnRepo.riskSignalsForSubjectDay(subjectKey, dayStart, dayEnd);
                        var windowStart = dayStart.minus(java.time.Duration.ofHours(72));
                        var windowSignals = txnRepo.riskSignalsForSubjectWindow(subjectKey, windowStart, dayEnd);

                        var scoreResult = scoreWithExternalSignals(subjectKey, row, dayStart, dayEnd, daySignals,
                                        windowSignals);

                        ComplianceEvent event = mapper.toCtrEvent(
                                        row.getSubjectKey(),
                                        row.getSourceSubjectType(),
                                        row.getTxnDay(),
                                        row.getTotalCashAmount(),
                                        scoreResult.score(),
                                        idem);

                        ComplianceEvent saved = eventRepo.save(event);

                        // Emits the event for logging purposes.
                        complianceEmitter.emit(
                                        ComplianceEventLog.ctrCreated(saved.getEventId().toString(), idem,
                                                        "CTR_GENERATION",
                                                        Map.of(
                                                                        "subjectKey", row.getSubjectKey(),
                                                                        "day", row.getTxnDay().toString(),
                                                                        "totalCashAmount", row.getTotalCashAmount(),
                                                                        "score", scoreResult.score())));

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

                        // CTR -> SAR promotion thresholds
                        sarPromotionService.promoteFromCtr(saved, scoreResult);

                        created++;
                }

                return created;
        }

        @Transactional
        public int generateForSubjectDay(String subjectKey, LocalDate day) {
                Instant dayStart = day.atStartOfDay(ZoneOffset.UTC).toInstant();
                Instant dayEnd = day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

                List<CtrAggregationRow> rows = txnRepo.aggregateCtrCandidatesForSubjectDay(subjectKey, dayStart,
                                dayEnd);

                int created = 0;

                for (CtrAggregationRow row : rows) {
                        String idem = "CTR:" + subjectKey + ":" + day;

                        var existingOpt = eventRepo.findByIdempotencyKey(idem);
                        if (existingOpt.isPresent()) {
                                // CTR is immutable, but we still evaluate promotion using the latest day
                                // signals
                                ComplianceEvent existingCtr = existingOpt.get();

                                var daySignals = txnRepo.riskSignalsForSubjectDay(subjectKey, dayStart, dayEnd);
                                var windowStart = dayStart.minus(java.time.Duration.ofHours(72));
                                var windowSignals = txnRepo.riskSignalsForSubjectWindow(subjectKey, windowStart,
                                                dayEnd);

                                var scoreResult = scoreWithExternalSignals(subjectKey, row, dayStart, dayEnd,
                                                daySignals,
                                                windowSignals);

                                log.info("CTR already exists for subjectKey={}, day={}. Re-evaluating SAR promotion with score={}",
                                                subjectKey, day, scoreResult.score());

                                sarPromotionService.promoteFromCtr(existingCtr, scoreResult);

                                // No CTR created
                                continue;
                        }

                        var daySignals = txnRepo.riskSignalsForSubjectDay(subjectKey, dayStart, dayEnd);
                        var windowStart = dayStart.minus(java.time.Duration.ofHours(72));
                        var windowSignals = txnRepo.riskSignalsForSubjectWindow(subjectKey, windowStart, dayEnd);

                        var scoreResult = scoreWithExternalSignals(subjectKey, row, dayStart, dayEnd, daySignals,
                                        windowSignals);

                        ComplianceEvent event = mapper.toCtrEvent(
                                        row.getSubjectKey(),
                                        row.getSourceSubjectType(),
                                        row.getTxnDay(),
                                        row.getTotalCashAmount(),
                                        scoreResult.score(),
                                        idem);

                        ComplianceEvent saved = eventRepo.save(event);

                        complianceEmitter.emit(
                                        ComplianceEventLog.ctrCreated(saved.getEventId().toString(), idem,
                                                        "CTR_GENERATION",
                                                        Map.of(
                                                                        "subjectKey", row.getSubjectKey(),
                                                                        "day", row.getTxnDay().toString(),
                                                                        "totalCashAmount", row.getTotalCashAmount(),
                                                                        "score", scoreResult.score())));

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

                        // CTR -> SAR promotion thresholds
                        sarPromotionService.promoteFromCtr(saved, scoreResult);

                        created++;
                }

                return created;
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