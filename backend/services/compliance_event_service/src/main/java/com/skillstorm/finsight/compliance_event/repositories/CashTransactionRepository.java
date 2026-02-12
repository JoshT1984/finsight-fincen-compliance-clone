package com.skillstorm.finsight.compliance_event.repositories;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.skillstorm.finsight.compliance_event.models.CashTransaction;

public interface CashTransactionRepository extends JpaRepository<CashTransaction, Long> {

  /**
   * Aggregate all CTR candidates in a time window.
   * SubjectKey expression (consistent everywhere):
   * COALESCE(NULLIF(external_subject_key,''), CONCAT_WS(':', source_system,
   * source_subject_type, source_subject_id))
   */
  @Query(value = """
      SELECT
        COALESCE(NULLIF(external_subject_key,''),
                 CONCAT_WS(':', source_system, source_subject_type, source_subject_id)
        ) AS subjectKey,
        MAX(subject_name) AS subjectName,
        MAX(source_subject_type) AS sourceSubjectType,
        DATE(txn_time) AS txnDay,
        SUM(COALESCE(cash_in,0)) AS totalCashIn,
        SUM(COALESCE(cash_out,0)) AS totalCashOut,
        SUM(COALESCE(cash_in,0) + COALESCE(cash_out,0)) AS totalCashAmount,
        COUNT(*) AS txnCount
      FROM cash_transaction
      WHERE txn_time >= :fromTime
        AND txn_time < :toTime
      GROUP BY subjectKey, txnDay
      HAVING SUM(COALESCE(cash_in,0) + COALESCE(cash_out,0)) > 10000.00
      """, nativeQuery = true)
  List<CtrAggregationRow> aggregateCtrCandidates(
      @Param("fromTime") Instant fromTime,
      @Param("toTime") Instant toTime);

  /**
   * Aggregate CTR candidates for a specific subjectKey + day window.
   */
  @Query(value = """
      SELECT
        COALESCE(NULLIF(external_subject_key,''),
                 CONCAT_WS(':', source_system, source_subject_type, source_subject_id)
        ) AS subjectKey,
        MAX(subject_name) AS subjectName,
        MAX(source_subject_type) AS sourceSubjectType,
        DATE(txn_time) AS txnDay,
        SUM(COALESCE(cash_in,0)) AS totalCashIn,
        SUM(COALESCE(cash_out,0)) AS totalCashOut,
        SUM(COALESCE(cash_in,0) + COALESCE(cash_out,0)) AS totalCashAmount,
        COUNT(*) AS txnCount
      FROM cash_transaction
      WHERE txn_time >= :fromTime
        AND txn_time < :toTime
        AND COALESCE(NULLIF(external_subject_key,''),
                     CONCAT_WS(':', source_system, source_subject_type, source_subject_id)
            ) = :subjectKey
      GROUP BY subjectKey, txnDay
      HAVING SUM(COALESCE(cash_in,0) + COALESCE(cash_out,0)) > 10000.00
      """, nativeQuery = true)
  List<CtrAggregationRow> aggregateCtrCandidatesForSubjectDay(
      @Param("subjectKey") String subjectKey,
      @Param("fromTime") Instant fromTime,
      @Param("toTime") Instant toTime);

  /**
   * Txn ids for attaching to a CTR detail list.
   */
  @Query(value = """
      SELECT txn_id
      FROM cash_transaction
      WHERE txn_time >= :fromTime
        AND txn_time < :toTime
        AND COALESCE(NULLIF(external_subject_key,''),
                     CONCAT_WS(':', source_system, source_subject_type, source_subject_id)
            ) = :subjectKey
      ORDER BY txn_time ASC
      """, nativeQuery = true)
  List<Long> findTxnIdsForSubjectDay(
      @Param("subjectKey") String subjectKey,
      @Param("fromTime") Instant fromTime,
      @Param("toTime") Instant toTime);

  /**
   * Risk signals used for scoring the CTR.
   */
  @Query(value = """
      SELECT
        MAX(COALESCE(cash_in,0)) AS maxCashIn,
        MAX(COALESCE(cash_out,0)) AS maxCashOut,
        SUM(CASE WHEN COALESCE(cash_in,0) > 0 AND COALESCE(cash_in,0) < 10000 THEN 1 ELSE 0 END) AS cashInUnder10kCount,
        SUM(CASE WHEN COALESCE(cash_in,0) >= 9000 AND COALESCE(cash_in,0) < 10000 THEN 1 ELSE 0 END) AS cashInNearThresholdCount,
        COUNT(DISTINCT COALESCE(NULLIF(location,''), 'UNKNOWN')) AS distinctLocationCount,
        COUNT(*) AS txnCount
      FROM cash_transaction
      WHERE txn_time >= :fromTime
        AND txn_time < :toTime
        AND COALESCE(NULLIF(external_subject_key,''),
                     CONCAT_WS(':', source_system, source_subject_type, source_subject_id)
            ) = :subjectKey
      """, nativeQuery = true)
  CtrRiskSignalsRow riskSignalsForSubjectDay(
      @Param("subjectKey") String subjectKey,
      @Param("fromTime") Instant fromTime,
      @Param("toTime") Instant toTime);

  /**
   * Optional: window scoring (if your service uses it).
   */
  @Query(value = """
      SELECT
        MAX(COALESCE(cash_in,0)) AS maxCashIn,
        MAX(COALESCE(cash_out,0)) AS maxCashOut,
        SUM(CASE WHEN COALESCE(cash_in,0) > 0 AND COALESCE(cash_in,0) < 10000 THEN 1 ELSE 0 END) AS cashInUnder10kCount,
        SUM(CASE WHEN COALESCE(cash_in,0) >= 9000 AND COALESCE(cash_in,0) < 10000 THEN 1 ELSE 0 END) AS cashInNearThresholdCount,
        COUNT(DISTINCT COALESCE(NULLIF(location,''), 'UNKNOWN')) AS distinctLocationCount,
        COUNT(*) AS txnCount
      FROM cash_transaction
      WHERE txn_time >= :windowStart
        AND txn_time < :windowEnd
        AND COALESCE(NULLIF(external_subject_key,''),
                     CONCAT_WS(':', source_system, source_subject_type, source_subject_id)
            ) = :subjectKey
      """, nativeQuery = true)
  CtrRiskSignalsRow riskSignalsForSubjectWindow(
      @Param("subjectKey") String subjectKey,
      @Param("windowStart") Instant windowStart,
      @Param("windowEnd") Instant windowEnd);

  /**
   * Baseline: average daily total cash amount over a historical window.
   */
  @Query(value = """
      SELECT AVG(day_total) AS avgDaily
      FROM (
        SELECT DATE(txn_time) AS txnDay,
               SUM(COALESCE(cash_in,0) + COALESCE(cash_out,0)) AS day_total
        FROM cash_transaction
        WHERE txn_time >= :fromTime
          AND txn_time < :toTime
          AND COALESCE(NULLIF(external_subject_key,''),
                       CONCAT_WS(':', source_system, source_subject_type, source_subject_id)
              ) = :subjectKey
        GROUP BY DATE(txn_time)
      ) t
      """, nativeQuery = true)
  BigDecimal avgDailyCashTotalForSubject(
      @Param("subjectKey") String subjectKey,
      @Param("fromTime") Instant fromTime,
      @Param("toTime") Instant toTime);

  /**
   * Distinct locations for a subject/day.
   */
  @Query(value = """
      SELECT DISTINCT COALESCE(NULLIF(location,''), 'UNKNOWN') AS location
      FROM cash_transaction
      WHERE txn_time >= :fromTime
        AND txn_time < :toTime
        AND COALESCE(NULLIF(external_subject_key,''),
                     CONCAT_WS(':', source_system, source_subject_type, source_subject_id)
            ) = :subjectKey
      """, nativeQuery = true)
  List<String> distinctLocationsForSubjectDay(
      @Param("subjectKey") String subjectKey,
      @Param("fromTime") Instant fromTime,
      @Param("toTime") Instant toTime);
}