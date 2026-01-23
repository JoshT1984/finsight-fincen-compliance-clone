-- ======================================
-- SUSPECT REGISTRY SERVICE (System of Record)
-- ======================================
 
CREATE TABLE IF NOT EXISTS suspect (
  suspect_id BIGSERIAL PRIMARY KEY,
 
  -- stable identifiers for internal use
  primary_name VARCHAR(256) NOT NULL,
 
  -- optional, depends on scope
  dob DATE,
  ssn_last4 CHAR(4),
 
  risk_level VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN'
    CHECK (risk_level IN ('UNKNOWN','LOW','MEDIUM','HIGH')),
 
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
 
CREATE INDEX IF NOT EXISTS idx_suspect_name ON suspect(primary_name);
 
 
-- 1:M Suspect -> Aliases
CREATE TABLE IF NOT EXISTS suspect_alias (
  alias_id BIGSERIAL PRIMARY KEY,
  suspect_id BIGINT NOT NULL REFERENCES suspect(suspect_id) ON DELETE CASCADE,
 
  alias_name VARCHAR(256) NOT NULL,
  alias_type VARCHAR(32) NOT NULL DEFAULT 'AKA'
    CHECK (alias_type IN ('AKA','LEGAL','NICKNAME','BUSINESS')),
 
  is_primary BOOLEAN NOT NULL DEFAULT FALSE,
 
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
 
CREATE INDEX IF NOT EXISTS idx_alias_suspect ON suspect_alias(suspect_id);
CREATE INDEX IF NOT EXISTS idx_alias_name ON suspect_alias(alias_name);
 
 
-- Address master (dedupe potential)
CREATE TABLE IF NOT EXISTS address (
  address_id BIGSERIAL PRIMARY KEY,
 
  line1 VARCHAR(256) NOT NULL,
  line2 VARCHAR(256),
  city VARCHAR(128) NOT NULL,
  state VARCHAR(64),
  postal_code VARCHAR(32),
  country VARCHAR(64) NOT NULL DEFAULT 'US',
 
  -- optional: address hash for dedupe
  address_hash CHAR(64),
 
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 
  UNIQUE (line1, COALESCE(line2,''), city, COALESCE(state,''), COALESCE(postal_code,''), country)
);
 
CREATE INDEX IF NOT EXISTS idx_address_city ON address(city);
 
 
-- M:N Suspect <-> Address
CREATE TABLE IF NOT EXISTS suspect_address (
  suspect_id BIGINT NOT NULL REFERENCES suspect(suspect_id) ON DELETE CASCADE,
  address_id BIGINT NOT NULL REFERENCES address(address_id) ON DELETE RESTRICT,
 
  address_type VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN'
    CHECK (address_type IN ('HOME','WORK','MAILING','UNKNOWN')),
 
  is_current BOOLEAN NOT NULL DEFAULT TRUE,
 
  linked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 
  PRIMARY KEY (suspect_id, address_id)
);
 
CREATE INDEX IF NOT EXISTS idx_suspect_address_suspect ON suspect_address(suspect_id);
CREATE INDEX IF NOT EXISTS idx_suspect_address_address ON suspect_address(address_id);
 
 
-- Criminal org master
CREATE TABLE IF NOT EXISTS criminal_organization (
  org_id BIGSERIAL PRIMARY KEY,
  org_name VARCHAR(256) NOT NULL UNIQUE,
  org_type VARCHAR(64),
 
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
 
-- M:N Suspect <-> Criminal org
CREATE TABLE IF NOT EXISTS suspect_criminal_organization (
  suspect_id BIGINT NOT NULL REFERENCES suspect(suspect_id) ON DELETE CASCADE,
  org_id BIGINT NOT NULL REFERENCES criminal_organization(org_id) ON DELETE RESTRICT,
 
  role VARCHAR(64),
  linked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 
  PRIMARY KEY (suspect_id, org_id)
);
 
CREATE INDEX IF NOT EXISTS idx_suspect_org_suspect ON suspect_criminal_organization(suspect_id);
CREATE INDEX IF NOT EXISTS idx_suspect_org_org ON suspect_criminal_organization(org_id);
 
 
-- Updated-at trigger (same helper you already have)
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;
 
DROP TRIGGER IF EXISTS trg_suspect_updated_at ON suspect;
CREATE TRIGGER trg_suspect_updated_at
BEFORE UPDATE ON suspect
FOR EACH ROW EXECUTE FUNCTION set_updated_at();
 