package com.skillstorm.finsight.suspect_registry.models;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "criminal_organization")
public class Organization {
  
  @Id
  @Column(name = "org_id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @Column(name = "org_name", nullable = false, unique = true, length = 256)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "org_type", length = 32)
  private OrganizationType type;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<SuspectOrganization> suspects = new ArrayList<>();

  public Organization() {
  }

  public Organization(String name, OrganizationType type, Instant createdAt) {
    this.name = name;
    this.type = type;
    this.createdAt = createdAt;
  }

  public Organization(long id, String name, OrganizationType type, Instant createdAt) {
    this.id = id;
    this.name = name;
    this.type = type;
    this.createdAt = createdAt;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public OrganizationType getType() {
    return type;
  }

  public void setType(OrganizationType type) {
    this.type = type;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public List<SuspectOrganization> getSuspects() {
    return suspects;
  }

  public void setSuspects(List<SuspectOrganization> suspects) {
    this.suspects = suspects;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (id ^ (id >>> 32));
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    result = prime * result + ((createdAt == null) ? 0 : createdAt.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Organization other = (Organization) obj;
    if (id != other.id)
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    if (type == null) {
      if (other.type != null)
        return false;
    } else if (!type.equals(other.type))
      return false;
    if (createdAt == null) {
      if (other.createdAt != null)
        return false;
    } else if (!createdAt.equals(other.createdAt))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "Organization [id=" + id + ", name=" + name + ", type=" + type + ", createdAt=" + createdAt + "]";
  }
}
