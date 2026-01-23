package com.skillstorm.finsight.suspect_registry.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.skillstorm.finsight.suspect_registry.models.Alias;
import com.skillstorm.finsight.suspect_registry.repositories.AliasRepository;

@Service
public class AliasService {
  
  private final AliasRepository aliasRepository;
  
  public AliasService(AliasRepository aliasRepository) {
    this.aliasRepository = aliasRepository;
  }

  public List<Alias> getAllAliases() {
    return aliasRepository.findAll();
  }

  public Alias getAliasById(long id) {
    return aliasRepository.findById(id).orElse(null);
  }

  public Alias createAlias(Alias alias) {
    return aliasRepository.save(alias);
  }

  public Alias updateAlias(long id, Alias alias) {
    Alias existingAlias = aliasRepository.findById(id).orElse(null);
    if (existingAlias != null) {
      existingAlias.setAliasName(alias.getAliasName());
      existingAlias.setAliasType(alias.getAliasType());
      existingAlias.setSuspect(alias.getSuspect());
      return aliasRepository.save(existingAlias);
    }
    return null;
  }
}
