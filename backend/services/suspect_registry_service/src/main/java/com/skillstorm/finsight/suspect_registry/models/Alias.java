package com.skillstorm.finsight.suspect_registry.models;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "suspect_alias")
public class Alias {
  
  @Id
  @Column(name = "alias_id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @ManyToOne
  @JoinColumn(name = "suspect_id")
  private Suspect suspect;

  @Column(name = "alias_name")
  private String aliasName;

  @Column(name = "alias_type")
  private String aliasType;

  @Column(name = "is_primary")
  private boolean isPrimary;

  @CreationTimestamp
  @Column(name = "created_at")
  private Instant createdAt;

  // all-args, no-args, all-but-id constructors, getters, setters, hashCode, equals, toString
  
  public Alias() {
  }

  public Alias(Suspect suspect, String aliasName, String aliasType, boolean isPrimary, Instant createdAt) {
    this.suspect = suspect;
    this.aliasName = aliasName;
    this.aliasType = aliasType;
    this.isPrimary = isPrimary;
    this.createdAt = createdAt;
  }

  public Alias(long id, Suspect suspect, String aliasName, String aliasType, boolean isPrimary, Instant createdAt) {
    this.id = id;
    this.suspect = suspect;
    this.aliasName = aliasName;
    this.aliasType = aliasType;
    this.isPrimary = isPrimary;
    this.createdAt = createdAt;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public Suspect getSuspect() {
    return suspect;
  }

  public void setSuspect(Suspect suspect) {
    this.suspect = suspect;
  }

  public String getAliasName() {
    return aliasName;
  }

  public void setAliasName(String aliasName) {
    this.aliasName = aliasName;
  }

  public String getAliasType() {
    return aliasType;
  }

  public void setAliasType(String aliasType) {
    this.aliasType = aliasType;
  }

  public boolean isPrimary() {
    return isPrimary;
  }

  public void setPrimary(boolean isPrimary) {
    this.isPrimary = isPrimary;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (id ^ (id >>> 32));
    result = prime * result + ((suspect == null) ? 0 : suspect.hashCode());
    result = prime * result + ((aliasName == null) ? 0 : aliasName.hashCode());
    result = prime * result + ((aliasType == null) ? 0 : aliasType.hashCode());
    result = prime * result + (isPrimary ? 1231 : 1237);
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
    Alias other = (Alias) obj;
    if (id != other.id)
      return false;
    if (suspect == null) {
      if (other.suspect != null)
        return false;
    } else if (!suspect.equals(other.suspect))
      return false;
    if (aliasName == null) {
      if (other.aliasName != null)
        return false;
    } else if (!aliasName.equals(other.aliasName))
      return false;
    if (aliasType == null) {
      if (other.aliasType != null)
        return false;
    } else if (!aliasType.equals(other.aliasType))
      return false;
    if (isPrimary != other.isPrimary)
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
    return "Alias [id=" + id + ", suspect=" + suspect + ", aliasName=" + aliasName + ", aliasType=" + aliasType
        + ", isPrimary=" + isPrimary + ", createdAt=" + createdAt + "]";
  }
 
}
