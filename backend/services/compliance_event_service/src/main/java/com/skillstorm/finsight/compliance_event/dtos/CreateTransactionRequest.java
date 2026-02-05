package com.skillstorm.finsight.compliance_event.dtos;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateTransactionRequest(
        String sourceSystem,
        String sourceTxnId,
        String externalSubjectKey,
        String sourceSubjectType,
        String sourceSubjectId,
        String subjectName,
        Instant txnTime,
        BigDecimal cashIn,
        BigDecimal cashOut,
        String currency,
        String channel,
        String location
) {}
