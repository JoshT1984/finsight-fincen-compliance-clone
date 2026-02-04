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
    @Param("toTime") Instant toTime
  );

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
    @Param("toTime") Instant toTime
  );
}
