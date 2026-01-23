package com.skillstorm.finsight.documents_cases.repositories;

import com.skillstorm.finsight.documents_cases.models.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
}
