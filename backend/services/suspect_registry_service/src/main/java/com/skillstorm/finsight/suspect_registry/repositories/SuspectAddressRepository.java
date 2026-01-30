package com.skillstorm.finsight.suspect_registry.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.skillstorm.finsight.suspect_registry.models.SuspectAddress;
import com.skillstorm.finsight.suspect_registry.models.SuspectAddressId;

@Repository
public interface SuspectAddressRepository extends JpaRepository<SuspectAddress, SuspectAddressId> {

  List<SuspectAddress> findBySuspectIdOrderByLinkedAtDesc(long suspectId);
}
