package com.skillstorm.finsight.suspect_registry.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.skillstorm.finsight.suspect_registry.models.Organization;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
  
  Optional<Organization> findByName(String name);
}
