package com.skillstorm.finsight.documents_cases.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.skillstorm.finsight.documents_cases.models.CaseFile;

@Repository
public interface CaseFileRepository extends JpaRepository<CaseFile, Long> {

    Optional<CaseFile> findBySarId(Long sarId);

    Optional<CaseFile> findByCtrId(Long ctrId);

    @Query("SELECT c FROM CaseFile c WHERE c.status = 'REFERRED' OR (c.status = 'CLOSED' AND c.referredAt IS NOT NULL)")
    List<CaseFile> findVisibleToLawEnforcement();
}
