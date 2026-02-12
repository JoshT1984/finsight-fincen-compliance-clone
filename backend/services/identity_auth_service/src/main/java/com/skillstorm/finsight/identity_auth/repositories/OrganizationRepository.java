package com.skillstorm.finsight.identity_auth.repositories;

import com.skillstorm.finsight.identity_auth.models.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, String> {
    Optional<Organization> findByName(String name);
}
