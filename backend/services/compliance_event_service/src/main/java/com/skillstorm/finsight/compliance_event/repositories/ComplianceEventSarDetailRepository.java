package com.skillstorm.finsight.compliance_event.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.skillstorm.finsight.compliance_event.models.ComplianceEventSarDetail;

@Repository
public interface ComplianceEventSarDetailRepository extends JpaRepository<ComplianceEventSarDetail, Long> {

  Optional<ComplianceEventSarDetail> findByEventId(Long eventId);

  /**
   * Returns true if a SAR exists whose form_data references the CTR id via:
   * - $.fromCtrEventId
   * - $.promotedFromCtrEventId
   *
   * Uses JSON_UNQUOTE(JSON_EXTRACT(...)) so it works whether the JSON stores the
   * id as number or string.
   */
  @Query(value = """
      SELECT CASE WHEN COUNT(1) > 0 THEN TRUE ELSE FALSE END
      FROM compliance_event_sar_detail d
      WHERE
        JSON_UNQUOTE(JSON_EXTRACT(d.form_data, '$.fromCtrEventId')) = CAST(:ctrId AS CHAR)
        OR JSON_UNQUOTE(JSON_EXTRACT(d.form_data, '$.promotedFromCtrEventId')) = CAST(:ctrId AS CHAR)
      """, nativeQuery = true)
  boolean existsSarForCtr(@Param("ctrId") Long ctrId);
}
