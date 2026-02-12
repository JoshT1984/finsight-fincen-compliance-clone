package com.skillstorm.finsight.identity_auth.services;

import com.skillstorm.finsight.identity_auth.models.Organization;
import com.skillstorm.finsight.identity_auth.repositories.OrganizationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    public OrganizationService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    public List<Organization> findAll() {
        return organizationRepository.findAll();
    }

    public Optional<Organization> findById(String id) {
        return organizationRepository.findById(id);
    }

    public Optional<Organization> findByName(String name) {
        return organizationRepository.findByName(name);
    }

    public Organization save(Organization organization) {
        return organizationRepository.save(organization);
    }

    public void deleteById(String id) {
        organizationRepository.deleteById(id);
    }
}
