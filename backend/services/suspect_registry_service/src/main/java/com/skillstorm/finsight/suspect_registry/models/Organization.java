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
@Table(name = "criminal_organization")
public class Organization {
  
  @Id
  @Column(name = "org_id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @Column(name = "org_name")
  private String name;

  @Column(name = "org_type")
  private String type;

  @CreationTimestamp
  @Column(name = "created_at")
  private Instant createdAt;

  // all-args, no-args, all-but-id constructors, getters, setters, hashCode, equals, toString
}
