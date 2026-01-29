package com.skillstorm.finsight.suspect_registry.models;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "suspect_address")
@IdClass(SuspectAddressId.class)
public class SuspectAddress {
  
  @Id
  @Column(name = "suspect_id", nullable = false)
  private long suspectId;

  @Id
  @Column(name = "address_id", nullable = false)
  private long addressId;

  @ManyToOne
  @JoinColumn(name = "suspect_id", insertable = false, updatable = false)
  private Suspect suspect;

  @ManyToOne
  @JoinColumn(name = "address_id", insertable = false, updatable = false)
  private Address address;

  @Enumerated(EnumType.STRING)
  @Column(name = "address_type", nullable = false, length = 32)
  private AddressType addressType;

  @Column(name = "is_current", nullable = false)
  private boolean isCurrent;

  @CreationTimestamp
  @Column(name = "linked_at", nullable = false)
  private Instant linkedAt;

  public SuspectAddress() {
  }

  public SuspectAddress(long suspectId, long addressId, AddressType addressType, boolean isCurrent, Instant linkedAt) {
    this.suspectId = suspectId;
    this.addressId = addressId;
    this.addressType = addressType;
    this.isCurrent = isCurrent;
    this.linkedAt = linkedAt;
  }

  public SuspectAddress(Suspect suspect, Address address, AddressType addressType, boolean isCurrent, Instant linkedAt) {
    this.suspectId = suspect != null ? suspect.getId() : 0;
    this.addressId = address != null ? address.getId() : 0;
    this.suspect = suspect;
    this.address = address;
    this.addressType = addressType;
    this.isCurrent = isCurrent;
    this.linkedAt = linkedAt;
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

  public Suspect getSuspect() {
    return suspect;
  }

  public void setSuspect(Suspect suspect) {
    this.suspect = suspect;
    if (suspect != null) {
      this.suspectId = suspect.getId();
    }
  }

  public Address getAddress() {
    return address;
  }

  public void setAddress(Address address) {
    this.address = address;
    if (address != null) {
      this.addressId = address.getId();
    }
  }

  public AddressType getAddressType() {
    return addressType;
  }

  public void setAddressType(AddressType addressType) {
    this.addressType = addressType;
  }

  public boolean isCurrent() {
    return isCurrent;
  }

  public void setCurrent(boolean isCurrent) {
    this.isCurrent = isCurrent;
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
    result = prime * result + (int) (addressId ^ (addressId >>> 32));
    result = prime * result + ((addressType == null) ? 0 : addressType.hashCode());
    result = prime * result + (isCurrent ? 1231 : 1237);
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
    SuspectAddress other = (SuspectAddress) obj;
    if (suspectId != other.suspectId)
      return false;
    if (addressId != other.addressId)
      return false;
    if (addressType == null) {
      if (other.addressType != null)
        return false;
    } else if (!addressType.equals(other.addressType))
      return false;
    if (isCurrent != other.isCurrent)
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
    return "SuspectAddress [suspectId=" + suspectId + ", addressId=" + addressId + ", addressType=" + addressType
        + ", isCurrent=" + isCurrent + ", linkedAt=" + linkedAt + "]";
  }
}
