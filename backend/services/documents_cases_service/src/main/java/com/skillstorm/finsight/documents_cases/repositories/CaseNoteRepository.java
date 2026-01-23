package com.skillstorm.finsight.documents_cases.repositories;

import com.skillstorm.finsight.documents_cases.models.CaseNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaseNoteRepository extends JpaRepository<CaseNote, Long> {
    
    List<CaseNote> findByCaseIdOrderByCreatedAtAsc(Long caseId);
}
