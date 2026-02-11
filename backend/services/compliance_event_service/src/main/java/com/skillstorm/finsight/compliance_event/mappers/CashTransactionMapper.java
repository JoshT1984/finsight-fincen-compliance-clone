package com.skillstorm.finsight.compliance_event.mappers;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.skillstorm.finsight.compliance_event.dtos.CreateTransactionRequest;
import com.skillstorm.finsight.compliance_event.dtos.TransactionResponse;
import com.skillstorm.finsight.compliance_event.models.CashTransaction;

@Component
public class CashTransactionMapper {

    public CashTransaction toEntity(CreateTransactionRequest r) {
        if (r == null) {
            return null;
        }

        CashTransaction t = new CashTransaction();
        t.setSourceSystem(defaultIfBlank(r.sourceSystem(), "TXN_SEED"));
        t.setSourceTxnId(nullIfBlank(r.sourceTxnId()));

        // ✅ critical: store blank as NULL so COALESCE works as intended
        t.setExternalSubjectKey(nullIfBlank(r.externalSubjectKey()));

        t.setSourceSubjectType(nullIfBlank(r.sourceSubjectType()));
        t.setSourceSubjectId(nullIfBlank(r.sourceSubjectId()));
        t.setSubjectName(nullIfBlank(r.subjectName()));
        t.setTxnTime(r.txnTime());

        // ✅ harden against nulls coming from UI
        t.setCashIn(r.cashIn() != null ? r.cashIn() : BigDecimal.ZERO);
        t.setCashOut(r.cashOut() != null ? r.cashOut() : BigDecimal.ZERO);

        t.setCurrency(defaultIfBlank(r.currency(), "USD"));
        t.setChannel(defaultIfBlank(r.channel(), "BRANCH"));
        t.setLocation(nullIfBlank(r.location()));
        return t;
    }

    public TransactionResponse toResponse(CashTransaction t) {
        if (t == null) {
            return null;
        }

        return new TransactionResponse(
                t.getTxnId(),
                t.getExternalSubjectKey(),
                t.getSourceSystem(),
                t.getSourceSubjectType(),
                t.getSourceSubjectId(),
                t.getSubjectName(),
                t.getTxnTime(),
                t.getCashIn(),
                t.getCashOut(),
                t.getLocation(),
                t.getCreatedAt());
    }

    private String defaultIfBlank(String v, String def) {
        return (v == null || v.isBlank()) ? def : v;
    }

    private String nullIfBlank(String v) {
        return (v == null || v.isBlank()) ? null : v;
    }
}
