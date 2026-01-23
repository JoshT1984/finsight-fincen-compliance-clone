package com.skillstorm.finsight.documents_cases.repositories;

import com.skillstorm.finsight.documents_cases.models.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
}
