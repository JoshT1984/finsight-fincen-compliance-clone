package com.skillstorm.finsight.suspect_registry.models;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "suspect")
public class Suspect {
  
  @Id
  @Column(name = "suspect_id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @Column(name = "primary_name", nullable = false, length = 256)
  private String primaryName;

  @Column()
  private LocalDate dob;

  @Column(name = "ssn_last4", length = 4)
  private String ssnLast4;

  @Column(name = "risk_level", nullable = false, length = 16)
  private String riskLevel;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @OneToMany(mappedBy = "suspect", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Alias> aliases = new ArrayList<>();

  @OneToMany(mappedBy = "suspect", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<SuspectAddress> addresses = new ArrayList<>();

  @OneToMany(mappedBy = "suspect", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<SuspectOrganization> organizations = new ArrayList<>();

  public Suspect() {
  }

  public Suspect(String primaryName, LocalDate dob, String ssnLast4, String riskLevel, Instant createdAt, Instant updatedAt) {
    this.primaryName = primaryName;
    this.dob = dob;
    this.ssnLast4 = ssnLast4;
    this.riskLevel = riskLevel;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Suspect(long id, String primaryName, LocalDate dob, String ssnLast4, String riskLevel, Instant createdAt, Instant updatedAt) {
    this.id = id;
    this.primaryName = primaryName;
    this.dob = dob;
    this.ssnLast4 = ssnLast4;
    this.riskLevel = riskLevel;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getPrimaryName() {
    return primaryName;
  }

  public void setPrimaryName(String primaryName) {
    this.primaryName = primaryName;
  }

  public LocalDate getDob() {
    return dob;
  }

  public void setDob(LocalDate dob) {
    this.dob = dob;
  }

  public String getSsnLast4() {
    return ssnLast4;
  }

  public void setSsnLast4(String ssnLast4) {
    this.ssnLast4 = ssnLast4;
  }

  public String getRiskLevel() {
    return riskLevel;
  }

  public void setRiskLevel(String riskLevel) {
    this.riskLevel = riskLevel;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public List<Alias> getAliases() {
    return aliases;
  }

  public void setAliases(List<Alias> aliases) {
    this.aliases = aliases;
  }

  public List<SuspectAddress> getAddresses() {
    return addresses;
  }

  public void setAddresses(List<SuspectAddress> addresses) {
    this.addresses = addresses;
  }

  public List<SuspectOrganization> getOrganizations() {
    return organizations;
  }

  public void setOrganizations(List<SuspectOrganization> organizations) {
    this.organizations = organizations;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (id ^ (id >>> 32));
    result = prime * result + ((primaryName == null) ? 0 : primaryName.hashCode());
    result = prime * result + ((dob == null) ? 0 : dob.hashCode());
    result = prime * result + ((ssnLast4 == null) ? 0 : ssnLast4.hashCode());
    result = prime * result + ((riskLevel == null) ? 0 : riskLevel.hashCode());
    result = prime * result + ((createdAt == null) ? 0 : createdAt.hashCode());
    result = prime * result + ((updatedAt == null) ? 0 : updatedAt.hashCode());
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
    Suspect other = (Suspect) obj;
    if (id != other.id)
      return false;
    if (primaryName == null) {
      if (other.primaryName != null)
        return false;
    } else if (!primaryName.equals(other.primaryName))
      return false;
    if (dob == null) {
      if (other.dob != null)
        return false;
    } else if (!dob.equals(other.dob))
      return false;
    if (ssnLast4 == null) {
      if (other.ssnLast4 != null)
        return false;
    } else if (!ssnLast4.equals(other.ssnLast4))
      return false;
    if (riskLevel == null) {
      if (other.riskLevel != null)
        return false;
    } else if (!riskLevel.equals(other.riskLevel))
      return false;
    if (createdAt == null) {
      if (other.createdAt != null)
        return false;
    } else if (!createdAt.equals(other.createdAt))
      return false;
    if (updatedAt == null) {
      if (other.updatedAt != null)
        return false;
    } else if (!updatedAt.equals(other.updatedAt))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "Suspect [id=" + id + ", primaryName=" + primaryName + ", dob=" + dob + ", ssnLast4=" + ssnLast4
        + ", riskLevel=" + riskLevel + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + "]";
  }

}
