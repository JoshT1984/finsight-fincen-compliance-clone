package com.skillstorm.finsight.suspect_registry.models;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "address")
public class Address {
  
  @Id
  @Column(name = "address_id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @Column(nullable = false, length = 256)
  private String line1;

  @Column(length = 256)
  private String line2;

  @Column(nullable = false, length = 128)
  private String city;

  @Column(length = 64)
  private String state;

  @Column(name = "postal_code", length = 32)
  private String postalCode;

  @Column(nullable = false, length = 64)
  private String country;

  @Column(name = "address_hash", length = 64)
  private String addressHash;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @OneToMany(mappedBy = "address", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<SuspectAddress> suspects = new ArrayList<>();

  public Address() {
  }

  public Address(String line1, String line2, String city, String state, String postalCode, String country, String addressHash, Instant createdAt) {
    this.line1 = line1;
    this.line2 = line2;
    this.city = city;
    this.state = state;
    this.postalCode = postalCode;
    this.country = country;
    this.addressHash = addressHash;
    this.createdAt = createdAt;
  }

  public Address(long id, String line1, String line2, String city, String state, String postalCode, String country, String addressHash, Instant createdAt) {
    this.id = id;
    this.line1 = line1;
    this.line2 = line2;
    this.city = city;
    this.state = state;
    this.postalCode = postalCode;
    this.country = country;
    this.addressHash = addressHash;
    this.createdAt = createdAt;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getLine1() {
    return line1;
  }

  public void setLine1(String line1) {
    this.line1 = line1;
  }

  public String getLine2() {
    return line2;
  }

  public void setLine2(String line2) {
    this.line2 = line2;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public String getPostalCode() {
    return postalCode;
  }

  public void setPostalCode(String postalCode) {
    this.postalCode = postalCode;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public String getAddressHash() {
    return addressHash;
  }

  public void setAddressHash(String addressHash) {
    this.addressHash = addressHash;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public List<SuspectAddress> getSuspects() {
    return suspects;
  }

  public void setSuspects(List<SuspectAddress> suspects) {
    this.suspects = suspects;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (id ^ (id >>> 32));
    result = prime * result + ((line1 == null) ? 0 : line1.hashCode());
    result = prime * result + ((line2 == null) ? 0 : line2.hashCode());
    result = prime * result + ((city == null) ? 0 : city.hashCode());
    result = prime * result + ((state == null) ? 0 : state.hashCode());
    result = prime * result + ((postalCode == null) ? 0 : postalCode.hashCode());
    result = prime * result + ((country == null) ? 0 : country.hashCode());
    result = prime * result + ((addressHash == null) ? 0 : addressHash.hashCode());
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
    Address other = (Address) obj;
    if (id != other.id)
      return false;
    if (line1 == null) {
      if (other.line1 != null)
        return false;
    } else if (!line1.equals(other.line1))
      return false;
    if (line2 == null) {
      if (other.line2 != null)
        return false;
    } else if (!line2.equals(other.line2))
      return false;
    if (city == null) {
      if (other.city != null)
        return false;
    } else if (!city.equals(other.city))
      return false;
    if (state == null) {
      if (other.state != null)
        return false;
    } else if (!state.equals(other.state))
      return false;
    if (postalCode == null) {
      if (other.postalCode != null)
        return false;
    } else if (!postalCode.equals(other.postalCode))
      return false;
    if (country == null) {
      if (other.country != null)
        return false;
    } else if (!country.equals(other.country))
      return false;
    if (addressHash == null) {
      if (other.addressHash != null)
        return false;
    } else if (!addressHash.equals(other.addressHash))
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
    return "Address [id=" + id + ", line1=" + line1 + ", line2=" + line2 + ", city=" + city + ", state=" + state
        + ", postalCode=" + postalCode + ", country=" + country + ", addressHash=" + addressHash + ", createdAt="
        + createdAt + "]";
  }
}
