package com.skillstorm.finsight.suspect_registry.models;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.skillstorm.finsight.suspect_registry.config.SsnEncryptor;
import com.skillstorm.finsight.suspect_registry.util.SsnHashUtil;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

  @Convert(converter = SsnEncryptor.class)
  @Column(name = "ssn", length = 255)
  private String ssn;

  @Column(name = "ssn_hash", length = 64)
  private String ssnHash;

  @Enumerated(EnumType.STRING)
  @Column(name = "risk_level", nullable = false, length = 16)
  private RiskLevel riskLevel;

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

  public Suspect(String primaryName, LocalDate dob, String ssn, RiskLevel riskLevel, Instant createdAt, Instant updatedAt) {
    this.primaryName = primaryName;
    this.dob = dob;
    this.ssn = ssn;
    this.riskLevel = riskLevel;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Suspect(long id, String primaryName, LocalDate dob, String ssn, RiskLevel riskLevel, Instant createdAt, Instant updatedAt) {
    this.id = id;
    this.primaryName = primaryName;
    this.dob = dob;
    this.ssn = ssn;
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

  public String getSsn() {
    return ssn;
  }

  public void setSsn(String ssn) {
    this.ssn = ssn;
    this.ssnHash = ssn != null && !ssn.isBlank() ? SsnHashUtil.hash(ssn) : null;
  }

  public String getSsnHash() {
    return ssnHash;
  }

  public void setSsnHash(String ssnHash) {
    this.ssnHash = ssnHash;
  }

  public RiskLevel getRiskLevel() {
    return riskLevel;
  }

  public void setRiskLevel(RiskLevel riskLevel) {
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
    result = prime * result + ((ssn == null) ? 0 : ssn.hashCode());
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
    if (ssn == null) {
      if (other.ssn != null)
        return false;
    } else if (!ssn.equals(other.ssn))
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
    return "Suspect [id=" + id + ", primaryName=" + primaryName + ", dob=" + dob + ", ssn=" + (ssn != null ? "***" : null)
        + ", riskLevel=" + riskLevel + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + "]";
  }

}
