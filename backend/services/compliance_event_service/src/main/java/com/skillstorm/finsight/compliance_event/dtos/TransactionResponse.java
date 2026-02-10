package com.skillstorm.finsight.compliance_event.dtos;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
                Long txnId,
                String externalSubjectKey,
                String sourceSystem,
                String sourceSubjectType,
                String sourceSubjectId,
                String subjectName,
                Instant txnTime,
                BigDecimal cashIn,
                BigDecimal cashOut,
                String location,
                Instant createdAt) {
}
