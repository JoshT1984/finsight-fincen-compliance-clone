package com.skillstorm.finsight.suspect_registry.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.skillstorm.finsight.suspect_registry.models.Suspect;

@Repository
public interface SuspectRepository extends JpaRepository<Suspect, Long> {
  
  Optional<Suspect> findBySsnHash(String ssnHash);

  List<Suspect> findByOrganizationsOrganizationId(Long orgId);
}
