package com.skillstorm.finsight.suspect_registry.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.skillstorm.finsight.suspect_registry.models.Suspect;
import com.skillstorm.finsight.suspect_registry.repositories.SuspectRepository;

@Service
public class SuspectService {
  private final SuspectRepository suspectRepository;

  public SuspectService(SuspectRepository suspectRepository) {
    this.suspectRepository = suspectRepository;
  }

  public List<Suspect> getAllSuspects() {
    return suspectRepository.findAll();
  }

  public Suspect getSuspectById(long id) {
    return suspectRepository.findById(id).orElse(null);
  }

  public Suspect createSuspect(Suspect suspect) {
    return suspectRepository.save(suspect);
  }
}
