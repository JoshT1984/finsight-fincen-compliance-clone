package com.skillstorm.finsight.compliance_event.repositories;

import java.math.BigDecimal;

public interface CtrRiskSignalsRow {
    BigDecimal getMaxCashIn();
    BigDecimal getMaxCashOut();
    Long getCashInUnder10kCount();
    Long getDistinctLocationCount();
    Long getTxnCount();
}