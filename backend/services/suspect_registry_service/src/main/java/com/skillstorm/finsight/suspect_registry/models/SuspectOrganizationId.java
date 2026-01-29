package com.skillstorm.finsight.suspect_registry.models;

import java.io.Serializable;
import java.util.Objects;

public class SuspectOrganizationId implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  private long suspectId;
  private long organizationId;

  public SuspectOrganizationId() {
  }

  public SuspectOrganizationId(long suspectId, long organizationId) {
    this.suspectId = suspectId;
    this.organizationId = organizationId;
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

  @Override
  public int hashCode() {
    return Objects.hash(organizationId, suspectId);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SuspectOrganizationId other = (SuspectOrganizationId) obj;
    return organizationId == other.organizationId && suspectId == other.suspectId;
  }
}
