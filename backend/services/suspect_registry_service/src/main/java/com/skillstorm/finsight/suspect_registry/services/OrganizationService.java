package com.skillstorm.finsight.suspect_registry.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.skillstorm.finsight.suspect_registry.models.Organization;
import com.skillstorm.finsight.suspect_registry.repositories.OrganizationRepository;

@Service
public class OrganizationService {
  
  private final OrganizationRepository organizationRepository;

  public OrganizationService(OrganizationRepository organizationRepository) {
    this.organizationRepository = organizationRepository;
  }

  public List<Organization> getAllOrganizations() {
    return organizationRepository.findAll();
  }

  public Organization getOrganizationById(long id) {
    return organizationRepository.findById(id).orElse(null);
  }

  public Organization createOrganization(Organization organization) {
    return organizationRepository.save(organization);
  }
}
