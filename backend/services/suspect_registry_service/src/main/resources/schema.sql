-- ======================================
-- SUSPECT REGISTRY SERVICE (System of Record)
-- MySQL 8.0+ (InnoDB)
-- ======================================

CREATE TABLE IF NOT EXISTS suspect (
  suspect_id BIGINT NOT NULL AUTO_INCREMENT,

  -- stable identifiers for internal use
  primary_name VARCHAR(256) NOT NULL,

  -- optional, depends on scope
  dob DATE,
  ssn VARCHAR(255), -- Encrypted full SSN (stored as base64-encoded encrypted string)
  ssn_hash CHAR(64), -- SHA-256 hash of normalized SSN (digits only) for uniqueness lookup

  risk_level VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN',
  CHECK (risk_level IN ('UNKNOWN','LOW','MEDIUM','HIGH')),

  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (suspect_id),
  KEY idx_suspect_name (primary_name),
  UNIQUE KEY uk_suspect_ssn_hash (ssn_hash)
) ENGINE=InnoDB;


-- 1:M Suspect -> Aliases
CREATE TABLE IF NOT EXISTS suspect_alias (
  alias_id BIGINT NOT NULL AUTO_INCREMENT,
  suspect_id BIGINT NOT NULL,

  alias_name VARCHAR(256) NOT NULL,
  alias_type VARCHAR(32) NOT NULL DEFAULT 'AKA',
  CHECK (alias_type IN ('AKA','LEGAL','NICKNAME','BUSINESS')),

  is_primary BOOLEAN NOT NULL DEFAULT FALSE,

  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (alias_id),
  KEY idx_alias_suspect (suspect_id),
  KEY idx_alias_name (alias_name),

  CONSTRAINT fk_alias_suspect
    FOREIGN KEY (suspect_id) REFERENCES suspect(suspect_id) ON DELETE CASCADE
) ENGINE=InnoDB;


-- Address master (dedupe potential)
CREATE TABLE IF NOT EXISTS address (
  address_id BIGINT NOT NULL AUTO_INCREMENT,

  line1 VARCHAR(256) NOT NULL,
  line2 VARCHAR(256),
  city VARCHAR(128) NOT NULL,
  state VARCHAR(64),
  postal_code VARCHAR(32),
  country VARCHAR(64) NOT NULL DEFAULT 'US',

  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  address_dedup_key VARCHAR(768)
    GENERATED ALWAYS AS (CONCAT_WS('|', line1, IFNULL(line2,''), city, IFNULL(state,''), IFNULL(postal_code,''), country)) STORED,

  PRIMARY KEY (address_id),
  UNIQUE KEY uk_address_dedup (address_dedup_key),
  KEY idx_address_city (city)
) ENGINE=InnoDB;


-- M:N Suspect <-> Address
CREATE TABLE IF NOT EXISTS suspect_address (
  suspect_id BIGINT NOT NULL,
  address_id BIGINT NOT NULL,

  address_type VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN',
  CHECK (address_type IN ('HOME','WORK','MAILING','UNKNOWN')),

  is_current BOOLEAN NOT NULL DEFAULT TRUE,

  linked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (suspect_id, address_id),
  KEY idx_suspect_address_suspect (suspect_id),
  KEY idx_suspect_address_address (address_id),

  CONSTRAINT fk_suspect_address_suspect
    FOREIGN KEY (suspect_id) REFERENCES suspect(suspect_id) ON DELETE CASCADE,
  CONSTRAINT fk_suspect_address_address
    FOREIGN KEY (address_id) REFERENCES address(address_id) ON DELETE RESTRICT
) ENGINE=InnoDB;


-- Criminal org master
CREATE TABLE IF NOT EXISTS criminal_organization (
  org_id BIGINT NOT NULL AUTO_INCREMENT,
  org_name VARCHAR(256) NOT NULL,
  org_type VARCHAR(32) DEFAULT 'OTHER',
  CHECK (org_type IN ('CARTEL','GANG','TERRORIST','FRAUD_RING','MONEY_LAUNDERING','OTHER')),

  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (org_id),
  UNIQUE KEY uk_org_name (org_name)
) ENGINE=InnoDB;


-- M:N Suspect <-> Criminal org
CREATE TABLE IF NOT EXISTS suspect_criminal_organization (
  suspect_id BIGINT NOT NULL,
  org_id BIGINT NOT NULL,

  role VARCHAR(64),
  linked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (suspect_id, org_id),
  KEY idx_suspect_org_suspect (suspect_id),
  KEY idx_suspect_org_org (org_id),

  CONSTRAINT fk_suspect_org_suspect
    FOREIGN KEY (suspect_id) REFERENCES suspect(suspect_id) ON DELETE CASCADE,
  CONSTRAINT fk_suspect_org_org
    FOREIGN KEY (org_id) REFERENCES criminal_organization(org_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- updated_at is maintained via DEFAULT ... ON UPDATE CURRENT_TIMESTAMP on suspect.
