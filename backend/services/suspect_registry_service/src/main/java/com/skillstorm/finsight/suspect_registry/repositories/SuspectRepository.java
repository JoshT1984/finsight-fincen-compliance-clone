package com.skillstorm.finsight.suspect_registry.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.skillstorm.finsight.suspect_registry.models.Suspect;

@Repository
public interface SuspectRepository extends JpaRepository<Suspect, Long> {
  
}
