package com.skillstorm.finsight.suspect_registry.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.skillstorm.finsight.suspect_registry.models.Address;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
  
  @Query("SELECT a FROM Address a WHERE a.line1 = :line1 " +
         "AND (a.line2 = :line2 OR (a.line2 IS NULL AND :line2 IS NULL)) " +
         "AND a.city = :city " +
         "AND (a.state = :state OR (a.state IS NULL AND :state IS NULL)) " +
         "AND (a.postalCode = :postalCode OR (a.postalCode IS NULL AND :postalCode IS NULL)) " +
         "AND a.country = :country")
  Optional<Address> findByAddressComponents(
      @Param("line1") String line1,
      @Param("line2") String line2,
      @Param("city") String city,
      @Param("state") String state,
      @Param("postalCode") String postalCode,
      @Param("country") String country);
}
