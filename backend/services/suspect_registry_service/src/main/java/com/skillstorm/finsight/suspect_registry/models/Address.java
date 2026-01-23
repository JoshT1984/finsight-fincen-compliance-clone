package com.skillstorm.finsight.suspect_registry.models;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "address")
public class Address {
  
  @Id
  @Column(name = "address_id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @Column()
  private String line1;

  @Column()
  private String line2;

  @Column()
  private String city;

  @Column()
  private String state;

  @Column(name = "postal_code")
  private String postalCode;

  @Column()
  private String country;

  @Column(name = "address_hash")
  private String addressHash;

  @CreationTimestamp
  @Column(name = "created_at")
  private Instant createdAt;

  // all-args, no-args, all-but-id constructors, getters, setters, hashCode, equals, toString
}
