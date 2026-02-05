package com.skillstorm.finsight.compliance_event.mappers;

import org.springframework.stereotype.Component;

import com.skillstorm.finsight.compliance_event.dtos.CreateTransactionRequest;
import com.skillstorm.finsight.compliance_event.dtos.TransactionResponse;
import com.skillstorm.finsight.compliance_event.models.CashTransaction;

@Component
public class CashTransactionMapper {

    public CashTransaction toEntity(CreateTransactionRequest r) {
        if (r == null) return null;

        CashTransaction t = new CashTransaction();
        t.setSourceSystem(defaultIfBlank(r.sourceSystem(), "TXN_SEED"));
        t.setSourceTxnId(r.sourceTxnId());
        t.setExternalSubjectKey(r.externalSubjectKey());
        t.setSourceSubjectType(r.sourceSubjectType());
        t.setSourceSubjectId(r.sourceSubjectId());
        t.setSubjectName(r.subjectName());
        t.setTxnTime(r.txnTime());
        t.setCashIn(r.cashIn());
        t.setCashOut(r.cashOut());
        t.setCurrency(defaultIfBlank(r.currency(), "USD"));
        t.setChannel(defaultIfBlank(r.channel(), "BRANCH"));
        t.setLocation(r.location());
        return t;
    }

    public TransactionResponse toResponse(CashTransaction t) {
        if (t == null) return null;

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
                t.getCreatedAt()
        );
    }

    private String defaultIfBlank(String v, String def) {
        return (v == null || v.isBlank()) ? def : v;
    }
}
