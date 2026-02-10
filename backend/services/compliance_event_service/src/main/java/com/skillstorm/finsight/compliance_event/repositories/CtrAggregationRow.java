package com.skillstorm.finsight.compliance_event.repositories;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface CtrAggregationRow {
  String getSubjectKey();

  String getSubjectName();

  String getSourceSubjectType();

  LocalDate getTxnDay();

  BigDecimal getTotalCashIn();

  BigDecimal getTotalCashOut();

  BigDecimal getTotalCashAmount();

  Long getTxnCount();
}