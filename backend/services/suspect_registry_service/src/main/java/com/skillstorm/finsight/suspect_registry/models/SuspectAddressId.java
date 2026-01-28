package com.skillstorm.finsight.suspect_registry.models;

import java.io.Serializable;
import java.util.Objects;

public class SuspectAddressId implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  private long suspectId;
  private long addressId;

  public SuspectAddressId() {
  }

  public SuspectAddressId(long suspectId, long addressId) {
    this.suspectId = suspectId;
    this.addressId = addressId;
  }

  public long getSuspectId() {
    return suspectId;
  }

  public void setSuspectId(long suspectId) {
    this.suspectId = suspectId;
  }

  public long getAddressId() {
    return addressId;
  }

  public void setAddressId(long addressId) {
    this.addressId = addressId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(addressId, suspectId);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SuspectAddressId other = (SuspectAddressId) obj;
    return addressId == other.addressId && suspectId == other.suspectId;
  }
}
