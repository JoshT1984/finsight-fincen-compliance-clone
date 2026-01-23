package com.skillstorm.finsight.documents_cases.repositories;

import com.skillstorm.finsight.documents_cases.models.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    
    List<Document> findByCtrId(Long ctrId);
    
    List<Document> findBySarId(Long sarId);
    
    List<Document> findByCaseId(Long caseId);
}
