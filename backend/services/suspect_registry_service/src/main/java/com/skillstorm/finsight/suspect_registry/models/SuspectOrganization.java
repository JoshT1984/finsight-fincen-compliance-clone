package com.skillstorm.finsight.suspect_registry.models;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "suspect_criminal_organization")
@IdClass(SuspectOrganizationId.class)
public class SuspectOrganization {
  
  @Id
  @Column(name = "suspect_id", nullable = false)
  private long suspectId;

  @Id
  @Column(name = "org_id", nullable = false)
  private long organizationId;

  @ManyToOne
  @JoinColumn(name = "suspect_id", insertable = false, updatable = false)
  private Suspect suspect;

  @ManyToOne
  @JoinColumn(name = "org_id", insertable = false, updatable = false)
  private Organization organization;

  @Column(name = "role", length = 64)
  private String role;

  @CreationTimestamp
  @Column(name = "linked_at", nullable = false)
  private Instant linkedAt;

  public SuspectOrganization() {
  }

  public SuspectOrganization(long suspectId, long organizationId, String role, Instant linkedAt) {
    this.suspectId = suspectId;
    this.organizationId = organizationId;
    this.role = role;
    this.linkedAt = linkedAt;
  }

  public SuspectOrganization(Suspect suspect, Organization organization, String role, Instant linkedAt) {
    this.suspectId = suspect != null ? suspect.getId() : 0;
    this.organizationId = organization != null ? organization.getId() : 0;
    this.suspect = suspect;
    this.organization = organization;
    this.role = role;
    this.linkedAt = linkedAt;
  }

  public long getSuspectId() {
    return suspectId;
  }

  public void setSuspectId(long suspectId) {
    this.suspectId = suspectId;
  }

  public long getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(long organizationId) {
    this.organizationId = organizationId;
  }

  public Suspect getSuspect() {
    return suspect;
  }

  public void setSuspect(Suspect suspect) {
    this.suspect = suspect;
    if (suspect != null) {
      this.suspectId = suspect.getId();
    }
  }

  public Organization getOrganization() {
    return organization;
  }

  public void setOrganization(Organization organization) {
    this.organization = organization;
    if (organization != null) {
      this.organizationId = organization.getId();
    }
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public Instant getLinkedAt() {
    return linkedAt;
  }

  public void setLinkedAt(Instant linkedAt) {
    this.linkedAt = linkedAt;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (suspectId ^ (suspectId >>> 32));
    result = prime * result + (int) (organizationId ^ (organizationId >>> 32));
    result = prime * result + ((role == null) ? 0 : role.hashCode());
    result = prime * result + ((linkedAt == null) ? 0 : linkedAt.hashCode());
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
    SuspectOrganization other = (SuspectOrganization) obj;
    if (suspectId != other.suspectId)
      return false;
    if (organizationId != other.organizationId)
      return false;
    if (role == null) {
      if (other.role != null)
        return false;
    } else if (!role.equals(other.role))
      return false;
    if (linkedAt == null) {
      if (other.linkedAt != null)
        return false;
    } else if (!linkedAt.equals(other.linkedAt))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "SuspectOrganization [suspectId=" + suspectId + ", organizationId=" + organizationId + ", role=" + role
        + ", linkedAt=" + linkedAt + "]";
  }
}
