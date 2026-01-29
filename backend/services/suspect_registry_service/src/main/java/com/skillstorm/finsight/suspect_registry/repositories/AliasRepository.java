package com.skillstorm.finsight.suspect_registry.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.skillstorm.finsight.suspect_registry.models.Alias;

@Repository
public interface AliasRepository extends JpaRepository<Alias, Long> {
  
  Optional<Alias> findBySuspectIdAndAliasName(Long suspectId, String aliasName);

  List<Alias> findBySuspect_Id(Long suspectId);
}
