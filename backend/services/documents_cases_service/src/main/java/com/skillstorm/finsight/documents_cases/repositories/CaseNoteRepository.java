package com.skillstorm.finsight.documents_cases.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.skillstorm.finsight.documents_cases.models.CaseNote;

@Repository
public interface CaseNoteRepository extends JpaRepository<CaseNote, Long> {

    List<CaseNote> findByCaseIdOrderByCreatedAtAsc(Long caseId);

    List<CaseNote> findByCaseIdIn(List<Long> caseIds);
}
