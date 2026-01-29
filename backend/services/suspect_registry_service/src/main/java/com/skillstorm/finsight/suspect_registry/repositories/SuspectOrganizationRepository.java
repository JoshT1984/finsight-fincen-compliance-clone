package com.skillstorm.finsight.suspect_registry.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.skillstorm.finsight.suspect_registry.models.SuspectOrganization;
import com.skillstorm.finsight.suspect_registry.models.SuspectOrganizationId;

@Repository
public interface SuspectOrganizationRepository extends JpaRepository<SuspectOrganization, SuspectOrganizationId> {
}
