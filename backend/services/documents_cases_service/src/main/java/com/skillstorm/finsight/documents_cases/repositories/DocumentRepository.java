package com.skillstorm.finsight.documents_cases.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.skillstorm.finsight.documents_cases.models.Document;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    @Query("SELECT DISTINCT d.caseId FROM Document d WHERE d.uploadedByUserId = :userId AND d.caseId IS NOT NULL")
    List<Long> findDistinctCaseIdsByUploadedByUserId(@Param("userId") String userId);

    List<Document> findByCtrId(Long ctrId);

    List<Document> findBySarId(Long sarId);

    List<Document> findByCaseId(Long caseId);

    List<Document> findByUploadedByUserId(String uploadedByUserId);

    List<Document> findByCaseIdIn(List<Long> caseIds);

    List<Document> findBySarIdIn(List<Long> sarIds);

    List<Document> findByCtrIdIn(List<Long> ctrIds);
}
