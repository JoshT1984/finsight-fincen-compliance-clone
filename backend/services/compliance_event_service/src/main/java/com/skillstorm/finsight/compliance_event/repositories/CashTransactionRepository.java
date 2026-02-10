package com.skillstorm.finsight.compliance_event.repositories;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.skillstorm.finsight.compliance_event.models.CashTransaction;

public interface CashTransactionRepository extends JpaRepository<CashTransaction, Long> {

  @Query(value = """
        SELECT
          COALESCE(
            external_subject_key,
            CONCAT(source_system, ':', source_subject_type, ':', source_subject_id)
          ) AS subjectKey,
          MAX(subject_name) AS subjectName,
          MAX(source_subject_type) AS sourceSubjectType,
          DATE(txn_time) AS txnDay,
          SUM(cash_in) AS totalCashIn,
          SUM(cash_out) AS totalCashOut,
          SUM(cash_in + cash_out) AS totalCashAmount,
          COUNT(*) AS txnCount
        FROM cash_transaction
        WHERE txn_time >= :fromTime
          AND txn_time < :toTime
        GROUP BY subjectKey, txnDay
        HAVING SUM(cash_in + cash_out) > 10000.00
      """, nativeQuery = true)
  List<CtrAggregationRow> aggregateCtrCandidates(
      @Param("fromTime") Instant fromTime,
      @Param("toTime") Instant toTime);

  @Query(value = """
        SELECT
          COALESCE(
            external_subject_key,
            CONCAT(source_system, ':', source_subject_type, ':', source_subject_id)
              ) AS subjectKey,
               MAX(subject_name) AS subjectName,
          DATE(txn_time) AS txnDay,
          SUM(cash_in) AS totalCashIn,
          SUM(cash_out) AS totalCashOut,
          SUM(cash_in + cash_out) AS totalCashAmount,
          COUNT(*) AS txnCount
        FROM cash_transaction
        WHERE txn_time >= :fromTime
          AND txn_time < :toTime
          AND (
            external_subject_key = :subjectKey
            OR CONCAT(source_system, ':', source_subject_type, ':', source_subject_id) = :subjectKey
          )
        GROUP BY subjectKey, txnDay
        HAVING SUM(cash_in + cash_out) > 10000.00
      """, nativeQuery = true)
  List<CtrAggregationRow> aggregateCtrCandidatesForSubjectDay(
      @Param("subjectKey") String subjectKey,
      @Param("fromTime") Instant fromTime,
      @Param("toTime") Instant toTime);

  @Query(value = """
        SELECT txn_id
        FROM cash_transaction
        WHERE txn_time >= :fromTime AND txn_time < :toTime
          AND (
            external_subject_key = :subjectKey
            OR CONCAT(source_system, ':', source_subject_type, ':', source_subject_id) = :subjectKey
          )
        ORDER BY txn_time ASC
      """, nativeQuery = true)
  List<Long> findTxnIdsForSubjectDay(
      @Param("subjectKey") String subjectKey,
      @Param("fromTime") Instant fromTime,
      @Param("toTime") Instant toTime);

  @Query(value = """
        SELECT
          MAX(cash_in) AS maxCashIn,
          MAX(cash_out) AS maxCashOut,
          SUM(CASE WHEN cash_in > 0 AND cash_in < 10000 THEN 1 ELSE 0 END) AS cashInUnder10kCount,
          COUNT(DISTINCT COALESCE(NULLIF(location,''), 'UNKNOWN')) AS distinctLocationCount,
          COUNT(*) AS txnCount
        FROM cash_transaction
        WHERE txn_time >= :fromTime AND txn_time < :toTime
          AND (
            external_subject_key = :subjectKey
            OR CONCAT(source_system, ':', source_subject_type, ':', source_subject_id) = :subjectKey
          )
      """, nativeQuery = true)
  CtrRiskSignalsRow riskSignalsForSubjectDay(
      @Param("subjectKey") String subjectKey,
      @Param("fromTime") Instant fromTime,
      @Param("toTime") Instant toTime);

  @Query(value = """
        SELECT
          MAX(cash_in) AS maxCashIn,
          MAX(cash_out) AS maxCashOut,
          SUM(CASE WHEN cash_in > 0 AND cash_in < 10000 THEN 1 ELSE 0 END) AS cashInUnder10kCount,
          COUNT(DISTINCT COALESCE(NULLIF(location,''), 'UNKNOWN')) AS distinctLocationCount,
          COUNT(*) AS txnCount
        FROM cash_transaction
        WHERE txn_time >= :windowStart AND txn_time < :windowEnd
          AND (
            external_subject_key = :subjectKey
            OR CONCAT(source_system, ':', source_subject_type, ':', source_subject_id) = :subjectKey
          )
      """, nativeQuery = true)
  CtrRiskSignalsRow riskSignalsForSubjectWindow(
      @Param("subjectKey") String subjectKey,
      @Param("windowStart") Instant windowStart,
      @Param("windowEnd") Instant windowEnd);
}
