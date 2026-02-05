package com.skillstorm.finsight.compliance_event.services;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

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

    private final CashTransactionRepository txnRepo;
    private final ComplianceEventRepository eventRepo;
    private final ComplianceEventCtrDetailRepository ctrDetailRepo;
    private final CtrGenerationMapper mapper;

    public CtrGenerationService(
            CashTransactionRepository txnRepo,
            ComplianceEventRepository eventRepo,
            ComplianceEventCtrDetailRepository ctrDetailRepo,
            CtrGenerationMapper mapper
    ) {
        this.txnRepo = txnRepo;
        this.eventRepo = eventRepo;
        this.ctrDetailRepo = ctrDetailRepo;
        this.mapper = mapper;
    }

    @Transactional
    public int generate(LocalDate from, LocalDate to) {
        Instant fromTime = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toTime = to.atStartOfDay(ZoneOffset.UTC).toInstant();

        List<CtrAggregationRow> rows =
                txnRepo.aggregateCtrCandidates(fromTime, toTime);

        int created = 0;

        for (CtrAggregationRow row : rows) {
            String subjectKey = row.getSubjectKey();
            LocalDate day = row.getTxnDay();
            String idem = "CTR:" + subjectKey + ":" + day;

            if (eventRepo.findByIdempotencyKey(idem).isPresent()) {
                continue;
            }

            ComplianceEvent event =
                    mapper.toCtrEvent(subjectKey, day, row.getTotalCashAmount(), idem);

            ComplianceEvent saved = eventRepo.save(event);

            Instant dayStart = day.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant dayEnd = day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

            List<Long> txnIds =
                    txnRepo.findTxnIdsForSubjectDay(subjectKey, dayStart, dayEnd);

            ctrDetailRepo.save(
    mapper.toCtrDetail(saved, row, txnIds)
);


            created++;
        }

        return created;
    }
}
