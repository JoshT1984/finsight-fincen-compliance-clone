package com.skillstorm.finsight.documents_cases.repositories;

import com.skillstorm.finsight.documents_cases.models.CaseFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CaseFileRepository extends JpaRepository<CaseFile, Long> {
    
    Optional<CaseFile> findBySarId(Long sarId);
}
